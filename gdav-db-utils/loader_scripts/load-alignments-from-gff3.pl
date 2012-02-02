#!/usr/bin/perl -w
#
# Description:
#
# Loads est-to-genome alignments which could be from any source, e.g.
# blastn, tblastx, exonerate est2genome or coding2genome
# but they must be in valid GFF3 format (see documentation)
#
# Usage: ./load-alignments-from-gff3.pl -assembly "AgamP3" GFF3_file
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
#   -quiet                   No output unless there are errors
#
#end

use DBI;
use Getopt::Long;

my $assembly = '';

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
	   "assembly=s"=>\$assembly,

	   "quiet"=>\$quiet,

	   "host|dbhost=s"=>\$host,
	   "port|dbport=i"=>\$port,
	   "database|dbname=s"=>\$database,
	   "socket|dbsocket=s"=>\$socket,
	   "user|dbuser=s"=>\$user,
	   "password|dbpassword=s"=>\$password,
	  );

usage() if ($help);

my $input_gff = shift;

die "Input GFF file not given or not found\n" unless ($input_gff && -s $input_gff);
die "You must provide an assembly name\ne.g. -assembly \"AgamP3\".\n" unless ($assembly);

my $dbh = opendb($host, $port, $database, $socket, $user, $password);

my $last_insert_id = $dbh->prepare("SELECT LAST_INSERT_ID()");

# spp to become assembly I think...
# will add score later...
my $insert_alig = $dbh->prepare("INSERT INTO alignment (model_id, spp, chr, start, end, strand, cigar, score, method) VALUES (?,?,?,?,?,?,?,?,?)");

my $update_cigar = $dbh->prepare("UPDATE alignment SET cigar = ? WHERE alignment_id = ?");

my $check_seq = $dbh->prepare("SELECT count(*) FROM model WHERE model_name = ?");

my $select_model_id = $dbh->prepare("SELECT model_id FROM model WHERE model_name = ? LIMIT 1"); # multiple hits not handled yet...


#
# read through once to check seqs are in db
#

unless ($quiet) {
  print "Checking file format and that ESTs are in database...\n";
}

open(GFF, $input_gff) || die;
while (<GFF>) {
  next if (/^#/);
  next unless (/\S/);

  my @cols = split /\t/, $_;
  grep chomp, @cols;

  my %attribs = parse_attribs($cols[8]);

  if ($attribs{Name}) {

    my $est_acc = $attribs{Name};
    $check_seq->execute($est_acc);
    my ($count) = $check_seq->fetchrow_array();

    die "EST or cluster '$est_acc' is not in the database.  Did you run load-ests-from-fasta.pl?\n" if ($count == 0);

    die "Multiple ESTs or clusters named '$est_acc' are in the database.  We cannot handle this at the moment, please contact the authors for help." if ($count > 1);
  }

  die "wrong number of columns on the following line:\n$_" unless (@cols == 9);

}

unless ($quiet) {
  print "Loading alignments into database...\n";
}

my ($first_alig_id, $last_alig_id, $model_id);

my %gffid2aligid; # store the alignment database id for each similarity/gene's GFF id
my %aligid2strand; # remember which strand it's on

my %exons; # exon info for each alignment id

open(GFF, $input_gff) || die;
while (<GFF>) {
  next if (/^#/);
  next unless (/\S/);

  my @cols = split /\t/, $_;
  grep chomp, @cols;

  my %attribs = parse_attribs($cols[8]);

  if (defined $attribs{Name} && /\t(gene|similarity)\t/i) {
    my $est_acc = $attribs{Name};

    ## lookup model_id in model table
    $select_model_id->execute($est_acc);
    ($model_id) = $select_model_id->fetchrow_array();
    die unless (defined $model_id);


    my ($chr, $method, $start, $end, $score, $strand) = ($cols[0], $cols[1], $cols[3], $cols[4], $cols[5], $cols[6]);
    # $score not used yet

    # fix the strand to an integer
    $strand = -1 if ($strand eq '-');
    $strand = 0 if ($strand eq '.');
    $strand = 1 if ($strand eq '+');

    # grab the cigar line if it's there
    my $cigar = $attribs{Cigar} || '';

#     warn "insert_alig->execute($model_id, $assembly, $chr, $start, $end, $strand, $cigar)\n";
		$insert_alig->execute($model_id, $assembly, $chr, $start, $end, $strand, $cigar, $score, $method);
		$last_insert_id->execute();
		my ($alig_id) = $last_insert_id->fetchrow_array();
		die "could not insert alignment into database - wrong permissions or disk full?\n" unless (defined $alig_id);

		###TEMP
    ###my $alig_id = $attribs{ID};

		# if ID attribute is present, store the database id for later addition of cigar line
		my $id = $attribs{ID};
		if (defined $id) {
			$gffid2aligid{$id} = $alig_id;
			$aligid2strand{$alig_id} = $strand;
		}

    $first_alig_id = $alig_id if (!defined $first_alig_id);
    $last_alig_id = $alig_id;

  } elsif (defined $attribs{ID} && defined $attribs{Parent} && /\t(exon)\t/i) {
    my $parent = $attribs{Parent};
    my $alig_id = $gffid2aligid{$parent};
    push @{$exons{$alig_id}}, [ $cols[3], $cols[4] ]; # just start, end
  }
}

close(GFF);


#
# now calculate the cigar lines if exons were provided in the GFF3 file
#

foreach my $alig_id (keys %exons) {
  # sort exons by start
  my @exons = sort { $a->[0] <=> $b->[0] } @{$exons{$alig_id}};

	# cigar line generating code expects exons in mRNA 'reading' order
	# so reverse them if they are on negative strand
	@exons = reverse(@exons) if ($aligid2strand{$alig_id} < 0);

	my $cigar = '';
	my ($last_exon_end, $last_exon_start);
	foreach my $exon (@exons) {
		my $this_exon_start = $exon->[0];
    my $this_exon_end = $exon->[1];

    if (defined $last_exon_end) {
      my $deletionlen = $this_exon_start - $last_exon_end - 1;
			# handle reverse strand correctly
			if ($deletionlen <= 0) {
				$deletionlen = $last_exon_start - $this_exon_end - 1;
			}
      $cigar .= "D$deletionlen";
    }
    my $exonlen = $this_exon_end-$this_exon_start+1;
    $cigar .= "M$exonlen";

    $last_exon_start = $this_exon_start;
    $last_exon_end = $this_exon_end;
	}

	$update_cigar->execute($cigar, $alig_id);
}

foreach $handle ($last_insert_id, $insert_alig, $check_seq, $select_model_id, $update_cigar) {
  $handle->finish();
}

$dbh->disconnect();

print "Done.\n\nAssuming no other inserts were taking place, the internal database ids created were $first_alig_id-$last_alig_id (in case you need to delete them and try again).\n";



#
# decode the final column of the GFF3 file
#
# also allow for slightly odd search2gff output in the style of:
#   Target=Sequence:SEQID,number,number
#

sub parse_attribs {
  my $text = shift;
  my @pairs = split /\s*;\s*/, $text;
  my %hash = map { split /\s*=\s*/, $_ } @pairs;

  if ($hash{Target} && $hash{Target} =~ /^Sequence:([^,]+)/) {
    my $name = $1;
    $hash{Name} = $name;
    delete $hash{Target};
  }

  return %hash;
}


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

