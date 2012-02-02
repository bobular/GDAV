#!/usr/bin/perl -w
#
#
# Description:
#
# Loads annotation data into the MySQL database.  You can load as many annotation
# sets as you like.
#
# Usage: ./load-annotations-from-tsv.pl -description "InterProScan" TAB_DELIM_ANNOTS.tsv
#
# (does not read from standard input)
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
#   -quiet                   Do not ask questions if there are no errors.
#
#end

use DBI;
use Getopt::Long;

my $description = '';
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
	   "description=s"=>\$description,
	   "quiet"=>\$quiet,

	   "host|dbhost=s"=>\$host,
	   "port|dbport=i"=>\$port,
	   "database|dbname=s"=>\$database,
	   "socket|dbsocket=s"=>\$socket,
	   "user|dbuser=s"=>\$user,
	   "password|dbpassword=s"=>\$password,
	  );

usage() if ($help);

die "you must provide a description for this annotation set.\ne.g. -description \"InterProScan (excluding TMHMM,SigPep,Coils,Seg)\"\n" unless ($description);

my $annotfile = shift;

my $MAX_VALUE_LENGTH = 250;

die "Please provide one tab delimited annotation file on the commandline\n" unless ($annotfile && -s $annotfile && @ARGV == 0);

my $dbh = opendb($host, $port, $database, $socket, $user, $password);

my $last_insert_id = $dbh->prepare("SELECT LAST_INSERT_ID()");

open(ANNOT, $annotfile) || die "can't read from $annotfile\n";

my $headers = <ANNOT>;
chomp($headers);
my @headers = split /\t/, $headers;
$headers[0] .= ' (EST/cluster ID column, this heading is ignored)';

die "Your file appears not to be tab-delimited.\n" unless (@headers > 1);

unless ($quiet) {
  print "Your file contains the following column headings:\n\n";
  for (my $i=0; $i<@headers; $i++) {
    printf "%-2d %s\n", $i+1, $headers[$i];
  }
  print "\nThe annotation set will be described as \"$description\".\n";

  print "\nIs this correct? (y/n then <enter>) ";
  my $answer = <STDIN>;
  die "Ok, exiting...\n" if ($answer !~ /^y/i);

  print "Please be patient while we verify your data.\n";
}

#
# read through the file to check all columns are present
#
# also check for entries in the model table (sequences, descriptions etc)
#
#

my $check_seq = $dbh->prepare("SELECT count(*) FROM model WHERE model_name = ?");

while (<ANNOT>) {
  # do not chomp here (or trailing empty columns are missed)
  next if (/^#/);
  next unless (/\w/);
  my @cols = split /\t/, $_;
  die "The following line does not contain the correct number of columns:\n\n$_\n\nNo data has been loaded\n" unless (@cols == @headers);

  my $est_acc = shift @cols;
  $check_seq->execute($est_acc);
  my ($count) = $check_seq->fetchrow_array();

  die "EST or cluster '$est_acc' is not in the database.  Did you run load-ests-from-fasta.pl?\n" if ($count == 0);

  die "Multiple ESTs or clusters named '$est_acc' are in the database.  We cannot handle this at the moment, please contact the authors for help." if ($count > 1);

}

#
# now we can throw away the first column heading
#

shift @headers;

#
# now the file is good, we can create the submission and column records
#

unless ($quiet) {
  print "Please be patient while we load your annotations into the database.\n";
}

my $insert_submission = $dbh->prepare("INSERT INTO submission (description) VALUES (?)");
$insert_submission->execute($description);
$last_insert_id->execute();
my ($submission_id) = $last_insert_id->fetchrow_array();
die "Failed to insert into database, please check disk space and permissions.\n" unless (defined $submission_id);

my $insert_colname = $dbh->prepare("INSERT INTO col (submission_id, title) VALUES (?,?)");
my @col_ids;
foreach my $heading (@headers) {
  $insert_colname->execute($submission_id, $heading);
  $last_insert_id->execute();
  my ($col_id) = $last_insert_id->fetchrow_array();
  die unless (defined $col_id);
  push @col_ids, $col_id;
}

#
# open again and read for real
#

my $insert_annot = $dbh->prepare("INSERT INTO annotation (model_id, submission_id, col_id, annotation_value) VALUES (?,?,?,?)");

my $select_model_id = $dbh->prepare("SELECT model_id FROM model WHERE model_name = ? LIMIT 1"); # multiple hits not handled yet...

open(ANNOT, $annotfile) || die "can't read from $annotfile\n";
my $skip = <ANNOT>;
while (<ANNOT>) {
  my @cols = split /\t/, $_;
  grep chomp, @cols;
  my $est_acc = shift @cols;

  ## lookup model_id in model table
  $select_model_id->execute($est_acc);
  my ($model_id) = $select_model_id->fetchrow_array();
  die unless (defined $model_id);

  ## insert annotations
  for (my $i=0; $i<@cols; $i++) {
    my $value = $cols[$i];

		if ($value =~ /\S/) {
			$value = substr($value, 0, $MAX_VALUE_LENGTH) if (length($value)>$MAX_VALUE_LENGTH);
		} else {
			# this should insert empty or whitespace columns as NULL
			undef $value;
		}

    $insert_annot->execute($model_id, $submission_id, $col_ids[$i], $value);
  }

}
close(ANNOT);


foreach $handle ($last_insert_id, $select_model_id, $check_seq, $insert_annot, $insert_colname, $insert_submission) {
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

