# Azure DevOps: signierter Release-Build

Die Pipeline in `azure-pipelines.yml` baut aus `main` (und aus `v*`-Tags) ein
signiertes APK und ein signiertes AAB und veröffentlicht beide als Pipeline-Artefakte.

FairTrack nutzt **Play App Signing**: Google hält den eigentlichen App-Signing-Key.
Wir signieren nur mit einem *Upload-Key* — den Key, den Google prüft, bevor es das
Bundle mit dem echten Key neu signiert. Verlierst du den Upload-Key, lässt er sich
über den Play-Support zurücksetzen; der App-Signing-Key dagegen wäre unersetzlich.

## Einmalige Einrichtung

### 1. Keystore als Secure File hinterlegen

Der Upload-Keystore liegt lokal als `upload-keystore.jks` im Projektstamm und ist
gitignored. In Azure DevOps:

**Pipelines → Library → Secure files → + Secure file** → `upload-keystore.jks`
hochladen und **exakt auf `fairtrack-upload-keystore.jks` benennen** (die Pipeline
referenziert diesen Namen).

Unter *Properties* des Secure File die Pipeline autorisieren („Authorize for use in
all pipelines" oder gezielt für diese Pipeline).

### 2. Variable Group anlegen

**Pipelines → Library → + Variable group** → Name: `fairtrack-signing`

| Variable | Wert | Secret |
|---|---|---|
| `KEYSTORE_PASSWORD` | `storePassword` aus `keystore.properties` | ja |
| `KEY_ALIAS` | `fairtrack-upload` | nein |
| `KEY_PASSWORD` | `keyPassword` aus `keystore.properties` | ja |

Das Schloss-Symbol markiert eine Variable als Secret. Danach ist ihr Wert nicht mehr
lesbar — die Werte also vorher aus einem Passwortmanager holen, nicht aus der UI.

Secrets werden von Azure DevOps bewusst **nicht** automatisch als Umgebungsvariablen
in Skripte gespiegelt. `azure-pipelines.yml` reicht sie darum explizit im `env:`-Block
des Build-Schritts durch. Wer dort eine Variable ergänzt, muss sie auch dort eintragen.

### 3. Pipeline erstellen

**Pipelines → New pipeline → Azure Repos Git** (bzw. GitHub) → Repo wählen →
**Existing Azure Pipelines YAML file** → `/azure-pipelines.yml`.

Beim ersten Lauf fragt Azure DevOps nach der Freigabe für Secure File und Variable
Group; einmal bestätigen.

## Wie das Signing zusammenhängt

`app/build.gradle.kts` bezieht das Signing-Material aus zwei Quellen:

1. `keystore.properties` im Projektstamm — der lokale Weg (gitignored,
   Vorlage: `keystore.properties.example`).
2. Umgebungsvariablen `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
   `KEY_PASSWORD` — der CI-Weg.

Fehlt beides, wird gar kein `release`-Signing-Config angelegt und der Release-Build
bleibt unsigniert, statt fehlzuschlagen. Das hält Checkouts ohne Keystore baubar
(Fremdbeiträge, Fork-CI). Kehrseite: ein kaputtes Secret in der Pipeline würde still
ein unsigniertes APK erzeugen. Genau dagegen läuft der Schritt
*„APK-Signatur verifizieren"* mit `apksigner verify` — er bricht den Lauf ab, wenn das
Artefakt keine gültige Signatur trägt.

`KEYSTORE_FILE` setzt die Pipeline auf den Pfad, unter dem `DownloadSecureFile@1` den
Keystore auf dem Agent ablegt (`$(keystoreFile.secureFilePath)`, ein absoluter Pfad in
einem temporären Verzeichnis, das nach dem Lauf gelöscht wird).

## Artefakte

| Artefakt | Pfad | Zweck |
|---|---|---|
| `fairtrack-apk` | `app/build/outputs/apk/release/app-release.apk` | Direktinstallation, Tester |
| `fairtrack-aab` | `app/build/outputs/bundle/release/app-release.aab` | Upload in die Play Console |

Das APK ist mit APK Signature Scheme v2 signiert. v1 (JAR signing) ist bei `minSdk 24`
überflüssig und wird vom Android Gradle Plugin korrekt weggelassen.

## Lokaler Release-Build

```bash
./gradlew assembleRelease   # signiertes APK
./gradlew bundleRelease     # signiertes AAB
```

Setzt `keystore.properties` mit gültigen Werten voraus.

## Der Keystore darf nicht verloren gehen

`upload-keystore.jks` und `keystore.properties` sind gitignored und existieren nur
lokal und als Secure File. Beides gehört in einen Passwortmanager oder ein
verschlüsseltes Backup. Ohne den Upload-Key kannst du keine Updates hochladen, bis
Google ihn zurückgesetzt hat.

Fingerprint des aktuellen Upload-Keys (SHA-256):

```
C2:82:51:33:33:5D:15:93:46:FF:93:92:3F:4A:DB:EA:B8:38:74:7B:80:E0:5B:70:CE:EA:61:B7:3C:96:F3:06
```

Prüfen mit:

```bash
keytool -list -keystore upload-keystore.jks
```

## Jira-Integration: Deployments im Projekt FAIR

Die Pipeline besteht aus zwei Stages: **Build** (immer, für `main` und `v*`-Tags)
und **Deploy** (nur für `v*`-Tags). Deploy läuft als *Deployment-Job* gegen das
Azure-DevOps-Environment `fairtrack-production` — nur solche Jobs meldet Azure
DevOps als Deployment an Jira.

Einmalige Einrichtung:

1. **Atlassian Marketplace → „Azure Pipelines for Jira"** in der Jira-Site
   installieren und mit der Azure-DevOps-Organisation verbinden
   (Jira: Einstellungen → Apps; dort die Azure-DevOps-Organisation autorisieren).
2. Das Environment `fairtrack-production` wird beim ersten Deploy-Lauf
   automatisch angelegt (Pipelines → Environments).
3. **Issue-Keys in Commit-Messages verwenden** — die Verknüpfung läuft über
   Commits: `FAIR-123: Barcode-Scanner repariert`. Nur Commits mit Issue-Key
   tauchen im Jira-Issue unter „Deployments" bzw. im Deployments-Board auf.

Ein Release-Deployment entsteht also durch: Commits mit `FAIR-…`-Keys mergen →
Tag `vX.Y.Z` pushen → Build + Deploy laufen → Jira zeigt das Deployment am
Issue und in der Deployments-Ansicht des Projekts FAIR.
