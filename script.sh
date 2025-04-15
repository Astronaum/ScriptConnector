#!/bin/bash

operation=$1
uid=$2
name=$3
username=$4
email=$5

# Définition des fichiers
usersFile="C:/Users/akabba-adm/Desktop/outputs/users.txt"
logFile="C:/Users/akabba-adm/Desktop/outputs/log.txt"

# Fonction de log
log_message() {
    message=$1
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    logMessage="$timestamp - $message"
    echo "$logMessage"
    echo "$logMessage" >> "$logFile"
}

# Vérification de l'opération
if [ -z "$operation" ]; then
    log_message "Erreur: L'opération est requise."
    exit 1
fi

# Création des fichiers s'ils n'existent pas
if [ ! -f "$usersFile" ]; then
    touch "$usersFile"
fi
if [ ! -f "$logFile" ]; then
    touch "$logFile"
fi

case "$operation" in
    "create")
        log_message "Received parameters: Name=$name, Email=$email"

        if [ -z "$username" ] || [ -z "$email" ]; then
            log_message "Erreur: name et email sont requis."
            exit 1
        fi

        # Générer un UID unique
        uid=$name

        # Vérifier si l'utilisateur existe déjà
        if grep -q "UID=$uid" "$usersFile"; then
            log_message "Erreur: Un utilisateur avec UID=$uid existe déjà."
            exit 1
        fi

        # Ajouter l'utilisateur au fichier
        userEntry="UID=$uid, Name=$name, Username=$username, Email=$email"
        echo "$userEntry" >> "$usersFile"
        log_message "Utilisateur ajouté: UID=$uid, Name=$name, Username=$username, Email=$email"
        ;;

    "search")
        if [ -z "$uid" ]; then
            # Retourner tous les utilisateurs
            allUsers=$(cat "$usersFile")
            if [ -n "$allUsers" ]; then
                log_message "Retour de tous les utilisateurs:"
                echo "$allUsers"
            else
                log_message "Aucun utilisateur trouvé."
                exit 1
            fi
        else
            # Recherche d'un utilisateur par UID
            result=$(grep "UID=$uid" "$usersFile")
            if [ -n "$result" ]; then
                log_message "Utilisateur trouvé: $result"
                echo "$result"
            else
                log_message "Utilisateur avec UID=$uid introuvable."
                exit 1
            fi
        fi
        ;;

    "delete")
        log_message "Delete operation called with UID=$uid"

        if [ -z "$uid" ]; then
            log_message "Erreur: UID est requis."
            exit 1
        fi

        # Lire le contenu du fichier dans un tableau
        fileContent=$(cat "$usersFile")

        # Vérifier si l'utilisateur existe
        userEntry=$(echo "$fileContent" | grep "UID=$uid")

        if [ -n "$userEntry" ]; then
            # Supprimer l'entrée correspondante
            newFileContent=$(echo "$fileContent" | grep -v "UID=$uid")

            # Écrire le contenu mis à jour dans le fichier
            echo "$newFileContent" > "$usersFile"

            log_message "Utilisateur supprimé: UID=$uid"
        else
            log_message "Erreur: Aucun utilisateur trouvé avec UID=$uid"
            exit 1
        fi
        ;;

    "update")
        log_message "Received update parameters - Name: $name, Username=$username, Email: $email, UID: $uid"

        if [ -z "$uid" ]; then
            log_message "Erreur: UID requis pour mettre à jour un utilisateur."
            exit 1
        fi

        # Vérifier si l'utilisateur existe
        userExists=$(grep "UID=$uid" "$usersFile")

        if [ -z "$userExists" ]; then
            log_message "Erreur: Aucun utilisateur trouvé avec UID=$uid. Impossible de mettre à jour."
            exit 1
        fi

        # Lire le contenu du fichier
        lines=$(cat "$usersFile")
        updated=false
        newLines=""

        # Traiter les lignes pour trouver l'UID correspondant et modifier l'entrée
        while IFS= read -r line; do
            if [[ "$line" =~ "UID=$uid" ]]; then
                # Extraire les détails existants
                existingName=$(echo "$line" | cut -d ',' -f 1 | cut -d '=' -f 2)
                existingUsrName=$(echo "$line" | cut -d ',' -f 2 | cut -d '=' -f 2)
                existingEmail=$(echo "$line" | cut -d ',' -f 3 | cut -d '=' -f 2)

                # Utiliser le nom existant sauf si un nouveau nom est fourni
                newName="${name:-$existingName}"
                newUsrName="${username:-$existingUsrName}"
                newEmail="${email:-$existingEmail}"

                # Préparer la nouvelle entrée avec les détails mis à jour
                newEntry="UID=$uid, Name=$newName, Username=$newUsrName, Email=$newEmail"

                log_message "Utilisateur mis à jour: $newEntry"
                updated=true

                # Ajouter l'entrée mise à jour au tableau de nouvelles lignes
                newLines="$newLines$newEntry\n"
            else
                # Sinon, garder la ligne inchangée
                newLines="$newLines$line\n"
            fi
        done <<< "$lines"

        # Après avoir traité toutes les lignes, écrire le contenu mis à jour
        if [ "$updated" = true ]; then
            echo -e "$newLines" > "$usersFile"
            log_message "Fichier mis à jour avec succès."
        else
            log_message "Aucune mise à jour effectuée, aucun utilisateur trouvé avec UID=$uid."
        fi
        ;;

    *)
        log_message "Erreur: Opération '$operation' non supportée."
        exit 1
        ;;
esac

exit 0
