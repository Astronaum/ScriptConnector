use strict;
use warnings;
use POSIX qw(strftime);

# Command-line arguments
my $operation = shift @ARGV;

# Initialize variables
my %args = (
    uid      => undef,
    name     => undef,
    username => undef,
    email    => undef
);

# Handle the operation and arguments
if ($operation eq 'delete') {
    # Le premier argument après 'delete' est l'UID
    $args{uid} = shift @ARGV;
} else {
    # Loop through args and assign values based on flags (for other operations)
    while (my $arg = shift @ARGV) {
        if ($arg eq "-uid") {
            $args{uid} = shift @ARGV;
        } elsif ($arg eq "-name") {
            $args{name} = shift @ARGV;
        } elsif ($arg eq "-username") {
            $args{username} = shift @ARGV;
        } elsif ($arg eq "-email") {
            $args{email} = shift @ARGV;
        }
    }
}

# Logging function
my $log_file = "C:/Users/akabba-adm/Desktop/outputs/log.txt";
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

# File paths
my $users_file = "C:/Users/akabba-adm/Desktop/outputs/users.txt";

# Ensure users file exists
unless (-e $users_file) {
    open(my $uf, '>', $users_file) or die "Cannot create users file: $!";
    close $uf;
}

# Process operations
if ($operation eq "create") {
    log_message("Received parameters: Name=$args{name}, Email=$args{email}");

    if (!$args{username} || !$args{email}) {
        log_message("Erreur: username et email sont requis.");
        exit 1;
    }

    # Use the username as UID for testing
    $args{uid} ||= $args{username};

    open(my $uf, '<', $users_file);
    while (<$uf>) {
        if (/UID=\Q$args{uid}\E/) {
            log_message("Erreur: Un utilisateur avec UID=$args{uid} existe déjà.");
            close $uf;
            exit 1;
        }
    }
    close $uf;

    my $entry = "UID=$args{uid}, Name=$args{name}, Username=$args{username}, Email=$args{email}";
    open($uf, '>>', $users_file);
    print $uf "$entry\n";
    close $uf;
    log_message("Utilisateur ajouté: $entry");

} elsif ($operation eq "search") {
    if (!$args{uid}) {
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
            if (/UID=\Q$args{uid}\E/) {
                chomp;
                log_message("Utilisateur trouvé: $_");
                print "$_\n";
                $found = 1;
                last;
            }
        }
        close $uf;
        unless ($found) {
            log_message("Utilisateur avec UID=$args{uid} introuvable.");
            exit 1;
        }
    }

} elsif ($operation eq 'delete') {
    log_message("Delete operation called with UID=$args{uid}");

    if (!$args{uid}) {
        log_message("Erreur: UID est requis.");
        exit 1;
    }

    # Read all lines from the file
    open(my $uf_read, '<', $users_file) or die "Erreur d'ouverture du fichier: $!";
    my @lines = <$uf_read>;
    close $uf_read;

    my $found = 0;
    my @filtered_lines;

    foreach my $line (@lines) {
        chomp $line;
        if ($line =~ /UID=\Q$args{uid}\E\b/) {
            $found = 1;
            next; # Skip this line
        }
        push @filtered_lines, "$line\n";
    }

    if ($found) {
        open(my $uf_write, '>', $users_file) or die "Erreur d'écriture dans le fichier: $!";
        print $uf_write @filtered_lines;
        close $uf_write;

        log_message("Utilisateur supprimé: UID=$args{uid}");
    } else {
        log_message("Erreur: Aucun utilisateur trouvé avec UID=$args{uid}");
        exit 1;
    }

} elsif ($operation eq "update") {
    log_message("Received update parameters - Name: $args{name}, Username=$args{username}, Email: $args{email}, UID: $args{uid}");

    if (!$args{uid}) {
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
        if ($line =~ /UID=\Q$args{uid}\E/) {
            my ($old_uid, $old_name, $old_username, $old_email) = $line =~ /UID=([^,]+), Name=([^,]+), Username=([^,]+), Email=(.+)/;

            my $new_name = $args{name} || $old_name;
            my $new_username = $args{username} || $old_username;
            my $new_email = $args{email} || $old_email;

            my $new_entry = "UID=$args{uid}, Name=$new_name, Username=$new_username, Email=$new_email";
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
        log_message("Aucune mise à jour effectuée, aucun utilisateur trouvé avec UID=$args{uid}.");
        exit 1;
    }

} else {
    log_message("Erreur: Opération '$operation' non supportée.");
    exit 1;
}

exit 0;
