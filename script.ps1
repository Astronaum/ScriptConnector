param (
    [string]$operation,
    [string]$uid,
    [string]$name,
    [string]$email
)

# Définition des fichiers
$usersFile = "C:\Users\akabba-adm\Desktop\outputs\users.txt"
$logFile = "C:\Users\akabba-adm\Desktop\outputs\log.txt"

function Log {
    param ([string]$message)
    $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    $logMessage = "$timestamp - $message"
    Write-Output $logMessage
    Add-Content -Path $logFile -Value $logMessage
}

# Vérification de l'opération
if (-not $operation) {
    Log "Erreur: L'opération est requise."
    exit 1
}

# Création des fichiers s'ils n'existent pas
if (-not (Test-Path $usersFile)) { New-Item -Path $usersFile -ItemType File | Out-Null }
if (-not (Test-Path $logFile)) { New-Item -Path $logFile -ItemType File | Out-Null }

switch ($operation) {
    "create" {
        Log "Received parameters: Name=$name, Email=$email"

        # Utiliser name comme UID
        if (-not $name -or -not $email) {
            Log "Erreur: name et email sont requis."
            exit 1
        }

        # Générer un UID unique
		$uid = [guid]::NewGuid().ToString()

        # Vérifier si l'utilisateur existe déjà
        if (Select-String -Path $usersFile -Pattern "UID=$uid") {
            Log "Erreur: Un utilisateur avec UID=$uid existe déjà."
            exit 1
        }

        # Ajouter l'utilisateur au fichier
        $userEntry = "UID=$uid, Name=$name, Email=$email"
        Add-Content -Path $usersFile -Value $userEntry
        Log "Utilisateur ajouté: UID=$uid, Name=$name, Email=$email"
    }

    "search" {
        if (-not $uid) {
            # Retourner tous les utilisateurs
            $allUsers = Get-Content -Path $usersFile
            if ($allUsers) {
                Log "Retour de tous les utilisateurs:"
                $allUsers | ForEach-Object {
                    Log "Utilisateur trouvé: $_"
                    Write-Output $_
                }
            } else {
                Log "Aucun utilisateur trouvé."
                exit 1
            }
        } else {
            # Recherche d'un utilisateur par UID
            $result = Select-String -Path $usersFile -Pattern "UID=$uid"
            if ($result) {
                Log "Utilisateur trouvé: $result"
                Write-Output $result
            } else {
                Log "Utilisateur avec UID=$uid introuvable."
                exit 1
            }
        }
    }

	"delete" {
		Log "Delete operation called with UID=$uid"

		if (-not $uid) {
			Log "Erreur: UID est requis."
			exit 1
		}

		# Read the file content into an array
		$fileContent = Get-Content $usersFile

		# Check if the user entry with UID exists
		$userEntry = $fileContent | Where-Object { $_ -match "UID=$uid" }

		if ($userEntry) {
			# Remove the matching line
			$fileContent = $fileContent | Where-Object { $_ -notmatch "UID=$uid" }

			# Write the updated content back to the file
			Set-Content -Path $usersFile -Value $fileContent

			Log "Utilisateur supprimé: UID=$uid"
		} else {
			Log "Erreur: Aucun utilisateur trouvé avec UID=$uid"
			exit 1
		}
	}

    "update" {
		Log "Received update parameters - Name: $name, Email: $email, UID: $uid"

		if (-not $uid) {
			Log "Erreur: UID requis pour mettre à jour un utilisateur."
			exit 1
		}

		# Vérifier si l'utilisateur existe
		$userExists = Select-String -Path $usersFile -Pattern "UID=$uid"

		if (-not $userExists) {
			Log "Erreur: Aucun utilisateur trouvé avec UID=$uid. Impossible de mettre à jour."
			exit 1
		}

		# Read the file content
		$lines = Get-Content -Path $usersFile
		$updated = $false
		$newLines = @()  # Array to hold the updated lines

		# Process the lines to find the matching UID and modify the corresponding entry
		foreach ($line in $lines) {
			if ($line -match "UID=$uid") {
				# Extract the existing user details to ensure we keep all the other information intact
				$existingName = ($line -split ", ")[1].Split("=")[1]
				$existingEmail = ($line -split ", ")[2].Split("=")[1]

				# Keep the existing name unless a new name is provided
				$newName = if ($name) { $name } else { $existingName }
				$newEmail = if ($email) { $email } else { $existingEmail }

				# Prepare the new entry with the updated (or unchanged) details
				$newEntry = "UID=$uid, Name=$newName, Email=$newEmail"

				# Log the update
				Log "Utilisateur mis à jour: $newEntry"
				$updated = $true

				# Add the updated entry to the new lines array
				$newLines += $newEntry
			} else {
				# If no match, keep the line unchanged
				$newLines += $line
			}
		}

		# After processing all lines, write back the updated content if any update was made
		if ($updated) {
			Set-Content -Path $usersFile -Value $newLines
			Log "Fichier mis à jour avec succès."
		} else {
			Log "Aucune mise à jour effectuée, aucun utilisateur trouvé avec UID=$uid."
		}
	}




    default {
        Log "Erreur: Opération '$operation' non supportée."
        exit 1
    }
}

exit 0
