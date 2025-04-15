import argparse
import os
import uuid
from datetime import datetime

users_file = r"C:\Users\akabba-adm\Desktop\outputs\users.txt"
log_file = r"C:\Users\akabba-adm\Desktop\outputs\log.txt"


def log(message):
    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    log_message = f"{timestamp} - {message}"
    print(log_message)
    with open(log_file, 'a') as f:
        f.write(log_message + '\n')


def ensure_files_exist():
    os.makedirs(os.path.dirname(users_file), exist_ok=True)
    if not os.path.exists(users_file):
        open(users_file, 'w').close()
    if not os.path.exists(log_file):
        open(log_file, 'w').close()


def create_user(name, username, email):
    if not name or not email or not username:
        log("Erreur: name, username et email sont requis.")
        exit(1)

    uid = str(uuid.uuid4())

    with open(users_file, 'r') as f:
        for line in f:
            if f"UID={uid}" in line:
                log(f"Erreur: Un utilisateur avec UID={uid} existe déjà.")
                exit(1)

    user_entry = f"UID={uid}, Name={name}, Username={username}, Email={email}"
    with open(users_file, 'a') as f:
        f.write(user_entry + '\n')

    log(f"Utilisateur ajouté: {user_entry}")


def search_user(uid=None):
    with open(users_file, 'r') as f:
        lines = f.readlines()

    if not uid:
        if lines:
            log("Retour de tous les utilisateurs:")
            for line in lines:
                log(f"Utilisateur trouvé: {line.strip()}")
                print(line.strip())
        else:
            log("Aucun utilisateur trouvé.")
            exit(1)
    else:
        found = False
        for line in lines:
            if f"UID={uid}" in line:
                log(f"Utilisateur trouvé: {line.strip()}")
                print(line.strip())
                found = True
                break
        if not found:
            log(f"Utilisateur avec UID={uid} introuvable.")
            exit(1)


def delete_user(uid):
    if not uid:
        log("Erreur: UID est requis.")
        exit(1)

    with open(users_file, 'r') as f:
        lines = f.readlines()

    updated_lines = [line for line in lines if f"UID={uid}" not in line]

    if len(updated_lines) != len(lines):
        with open(users_file, 'w') as f:
            f.writelines(updated_lines)
        log(f"Utilisateur supprimé: UID={uid}")
    else:
        log(f"Erreur: Aucun utilisateur trouvé avec UID={uid}")
        exit(1)


def update_user(uid, name=None, username=None, email=None):
    if not uid:
        log("Erreur: UID requis pour mettre à jour un utilisateur.")
        exit(1)

    with open(users_file, 'r') as f:
        lines = f.readlines()

    updated = False
    new_lines = []
    for line in lines:
        if f"UID={uid}" in line:
            parts = dict(item.strip().split("=") for item in line.strip().split(", "))
            new_entry = f"UID={uid}, Name={name or parts['Name']}, Username={username or parts['Username']}, Email={email or parts['Email']}"
            new_lines.append(new_entry + '\n')
            log(f"Utilisateur mis à jour: {new_entry}")
            updated = True
        else:
            new_lines.append(line)

    if updated:
        with open(users_file, 'w') as f:
            f.writelines(new_lines)
        log("Fichier mis à jour avec succès.")
    else:
        log(f"Aucune mise à jour effectuée, aucun utilisateur trouvé avec UID={uid}.")
        exit(1)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", help="Opération: create, search, update, delete")
    parser.add_argument("--uid", help="Identifiant utilisateur (UUID)")
    parser.add_argument("--name", help="Nom de l'utilisateur")
    parser.add_argument("--username", help="Username de l'utilisateur")
    parser.add_argument("--email", help="Email de l'utilisateur")

    args = parser.parse_args()

    ensure_files_exist()

    match args.operation:
        case "create":
            create_user(args.name, args.username, args.email)
        case "search":
            search_user(args.uid)
        case "delete":
            delete_user(args.uid)
        case "update":
            update_user(args.uid, args.name, args.username, args.email)
        case _:
            log(f"Erreur: Opération '{args.operation}' non supportée.")
            exit(1)

    exit(0)