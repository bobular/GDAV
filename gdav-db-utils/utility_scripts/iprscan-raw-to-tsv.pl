#!/usr/bin/perl -w
#
# 6-frame translated InterProScan output reformatter
#
# usage: ./iprscan-raw-to-tsv.pl -ignore Coil,Seg interpro.raw [interpro2.raw ...] > interpro.tsv
#
# this script makes some minor adjustments to the interproscan output
# 1. separates ORF suffix into its own column
# 2. makes ORF more human friendly
# 3. removes checksum column
# 4. adds column headings
# 5. removes unwanted analyses (e.g. Coil, Seg - see example usage)
# 6. converts NULL to '' (which will become NULL when loaded into MySQL)
#end

use Getopt::Long;

my $ignore_progs = '';

GetOptions("ignore_progs=s"=>\$ignore_progs,
	   "help"=>\$help,
	  );


usage() if ($help);

$ignore_progs =~ s/\W/\|/g;


my @headers = (

	   '#headers',
	   'translation',
##	   'checksum',
	   'length (AA)',
	   'analysis type',
	   'analysis domain ID',
	   'analysis description',
	   'query start',
	   'query end',
	   'e-value',
	   'true hit',
	   'date',
	   'InterPro ID',
	   'InterPro description',
	   'GO terms',
);

print join("\t", @headers,
	  )."\n";

my %nice_orf = (1=>'+1', 2=>'+2', 3=>'+3',
		4=>'-1', 5=>'-2', 6=>'-3');


while (<>) {
  chomp;
  # separate the ORF from the EST/contig ID
  s/_(\d_ORF)/\t$1/;
  s/(\d)_ORF(\d+)/ORF$2 ($nice_orf{$1})/;

  my @cols = split /\t/, $_;

  # remove checksum column
  splice @cols, 2, 1;

  next if ($cols[3] =~ /^$ignore_progs$/);

  # make up sometimes empty GO column
  push @cols, '', while (@cols < @headers);

	# convert NULL to '' (for mysql null)
	grep s/^NULL$//, @cols;

  print join("\t", @cols)."\n";
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
