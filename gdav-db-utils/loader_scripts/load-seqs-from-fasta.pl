#!/usr/bin/perl -w
#
#
# Description:
#
# Loads sequence IDs, sequences and descriptions into the MySQL database
#
# Usage: ./load-seqs-from-fasta.pl -species "Anopheles albimanus" FASTAFILE [FASTAFILES...]
#
# (also reads from standard input)
#
# MySQL database options:
#
#   -dbhost hostname (default=localhost)
#   -dbport port_number (default=3306)
#   -dbname database_name
#   -dbuser mysql_username
#   -dbpass mysql_password
#
# Additional/advanced options:
#
#   -dbsocket socket_path    Ask your system administrator if you get socket errors
#
#
#end

use DBI;
use Getopt::Long;

my $species = '';

my $quiet;

my $host = 'localhost';
my $port = 3306;
my $database = '';
my $socket = '';
my $user = '';
my $password = '';

$| = 1;

GetOptions(
	   "help"=>\$help,
	   "species=s"=>\$species,
	   "quiet"=>\$quiet,

	   "host|dbhost=s"=>\$host,
	   "port|dbport=i"=>\$port,
	   "database|dbname=s"=>\$database,
	   "socket|dbsocket=s"=>\$socket,
	   "user|dbuser=s"=>\$user,
	   "password|dbpassword=s"=>\$password,
	  );

usage() if ($help);

die "you must provide a species name.\ne.g. -species \"Anopheles gambiae\".\n" unless ($species);

my %seqs;
my %descrips;
my $id;
while (<>) {
  if (/^>(\S+)\s*(.*)/) {
    $id = $1;
    $descrips{$id} = $2 || '';
  } elsif (/>/) {
    die "could not parse FASTA header line at $_\n";
  } elsif ($id) {
    s/[^A-Za-z]//g;
    $seqs{$id} .= uc($_);
  }
}

die "No sequences read!!\n" unless (keys %seqs);

my $dbh = opendb($host, $port, $database, $socket, $user, $password);

my $last_insert_id = $dbh->prepare("SELECT LAST_INSERT_ID()");

my $insert_seq = $dbh->prepare("INSERT INTO model (model_name, spp, description, sequence) VALUES (?,?,?,?)");

foreach my $id (keys %seqs) {
  $insert_seq->execute($id, $species, $descrips{$id}, $seqs{$id});
  $last_insert_id->execute();
  my ($model_id) = $last_insert_id->fetchrow_array();
  die "could not insert $id!!\n" unless (defined $model_id);
}

foreach $handle ($last_insert_id, $insert_seq) {
  $handle->finish();
}

$dbh->disconnect();


sub opendb
{
  my ($host, $port, $database, $socket, $user, $password) = @_;

  my $sockopt = $socket ? ";mysql_socket=$socket" : '';
  my $dbh = DBI->connect("dbi:mysql:dbname=$database;host=$host;port=$port$sockopt", $user, $password );

  die "Can't connect to database - please check host, port, socket, database, username and password commandline options are correct.\n" unless ($dbh);

  return $dbh;
}


sub usage {
  if (open(ME, $0)) {
    my $ignore = <ME>;
    while(<ME>) {
      last if (/^#+end/);
      if (s/^#+//) {
	print;
      }
    }
    close(ME);
  }
  exit;
}

