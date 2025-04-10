use strict;
use warnings;
use POSIX qw(strftime);

# Command-line arguments
my $operation = shift @ARGV;

# Initialize variables
my ($uid, $name, $username, $email);

# Loop through args and assign values based on flags
while (my $arg = shift @ARGV) {
    if ($arg eq "-uid") {
        $uid = shift @ARGV;
    } elsif ($arg eq "-name") {
        $name = shift @ARGV;
    } elsif ($arg eq "-username") {
        $username = shift @ARGV;
    } elsif ($arg eq "-email") {
        $email = shift @ARGV;
    }
}

# File paths
my $users_file = "C:/Users/akabba-adm/Desktop/outputs/users.txt";
my $log_file = "C:/Users/akabba-adm/Desktop/outputs/log.txt";

# Logging function
sub log_message {
    my ($msg) = @_;
    my $timestamp = strftime "%Y-%m-%d %H:%M:%S", localtime;
    my $log_msg = "$timestamp - $msg";
    print "$log_msg\n";
    open(my $log_fh, '>>', $log_file) or die "Cannot open log file: $!";
    print $log_fh "$log_msg\n";
    close $log_fh;
}

# Ensure operation is provided
if (!$operation) {
    log_message("Erreur: L'opération est requise.");
    exit 1;
}

# Ensure files exist
unless (-e $users_file) {
    open(my $uf, '>', $users_file) or die "Cannot create users file: $!";
    close $uf;
}
unless (-e $log_file) {
    open(my $lf, '>', $log_file) or die "Cannot create log file: $!";
    close $lf;
}

if ($operation eq "create") {
    log_message("Received parameters: Name=$name, Email=$email");

    if (!$username || !$email) {
        log_message("Erreur: name et email sont requis.");
        exit 1;
    }

    # Use the username as UID for testing
    $uid = $username;

    open(my $uf, '<', $users_file);
    while (<$uf>) {
        if (/UID=\Q$uid\E/) {
            log_message("Erreur: Un utilisateur avec UID=$uid existe déjà.");
            close $uf;
            exit 1;
        }
    }
    close $uf;

    my $entry = "UID=$uid, Name=$name, Username=$username, Email=$email";
    open($uf, '>>', $users_file);
    print $uf "$entry\n";
    close $uf;
    log_message("Utilisateur ajouté: $entry");

} elsif ($operation eq "search") {
    if (!$uid) {
        open(my $uf, '<', $users_file);
        my @lines = <$uf>;
        close $uf;

        if (@lines) {
            log_message("Retour de tous les utilisateurs:");
            foreach my $line (@lines) {
                chomp $line;
                log_message("Utilisateur trouvé: $line");
                print "$line\n";
            }
        } else {
            log_message("Aucun utilisateur trouvé.");
            exit 1;
        }
    } else {
        open(my $uf, '<', $users_file);
        my $found = 0;
        while (<$uf>) {
            if (/UID=\Q$uid\E/) {
                chomp;
                log_message("Utilisateur trouvé: $_");
                print "$_\n";
                $found = 1;
                last;
            }
        }
        close $uf;
        unless ($found) {
            log_message("Utilisateur avec UID=$uid introuvable.");
            exit 1;
        }
    }

} elsif ($operation eq "delete") {
    log_message("Delete operation called with UID=$uid");

    if (!$uid) {
        log_message("Erreur: UID est requis.");
        exit 1;
    }

    open(my $uf, '<', $users_file);
    my @lines = <$uf>;
    close $uf;

    my $found = 0;
    @lines = grep {
        if (/UID=\Q$uid\E/) {
            $found = 1;
            0;
        } else {
            1;
        }
    } @lines;

    if ($found) {
        open($uf, '>', $users_file);
        print $uf @lines;
        close $uf;
        log_message("Utilisateur supprimé: UID=$uid");
    } else {
        log_message("Erreur: Aucun utilisateur trouvé avec UID=$uid");
        exit 1;
    }

} elsif ($operation eq "update") {
    log_message("Received update parameters - Name: $name, Username=$username, Email: $email, UID: $uid");

    if (!$uid) {
        log_message("Erreur: UID requis pour mettre à jour un utilisateur.");
        exit 1;
    }

    open(my $uf, '<', $users_file);
    my @lines = <$uf>;
    close $uf;

    my $updated = 0;
    my @new_lines;

    foreach my $line (@lines) {
        chomp $line;
        if ($line =~ /UID=\Q$uid\E/) {
            my ($old_uid, $old_name, $old_username, $old_email) = $line =~ /UID=([^,]+), Name=([^,]+), Username=([^,]+), Email=(.+)/;

            my $new_name = $name || $old_name;
            my $new_username = $username || $old_username;
            my $new_email = $email || $old_email;

            my $new_entry = "UID=$uid, Name=$new_name, Username=$new_username, Email=$new_email";
            push @new_lines, "$new_entry\n";

            log_message("Utilisateur mis à jour: $new_entry");
            $updated = 1;
        } else {
            push @new_lines, "$line\n";
        }
    }

    if ($updated) {
        open($uf, '>', $users_file);
        print $uf @new_lines;
        close $uf;
        log_message("Fichier mis à jour avec succès.");
    } else {
        log_message("Aucune mise à jour effectuée, aucun utilisateur trouvé avec UID=$uid.");
        exit 1;
    }

} else {
    log_message("Erreur: Opération '$operation' non supportée.");
    exit 1;
}

exit 0;
