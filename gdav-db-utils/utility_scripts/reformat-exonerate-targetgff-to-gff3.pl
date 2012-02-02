#!/usr/bin/perl -w
#
# usage: ./reformat-exonerate-targetgff-to-gff3.pl INFILE > OUTFILE
#
# (also reads from standard input)
#
# what it does:
#  * print out the 'gene' line in GFF3
#  * prints out the 'exon' lines in GFF3, link to parent gene
#
# INPUT FILE:   exonerate should have been run with (at least) the following options:
#               --model coding2genome  OR  --model est2genome
#               --showtargetgff yes
#
# it is OK to have other output formats in the exonerate output
#
#end

use Getopt::Long;
my $help;
GetOptions("help"=>\$help);

usage() if ($help);

my $est_acc;

my $id = "gene000000";
my $exon_id = "exon000000";

# my $alignment_serial_number = 0;

while (<>) {
  chomp;
  if (/exonerate:\w+2genome/ && /\tgene\t/ && /sequence (\S+)/) {
		$est_acc = $1;
		$id++;
    my @cols = split /\t/, $_;
    $cols[1] =~ s/chromosome:AgamP3:(\w+):.+/$1/; # if needed
    $cols[8] = "ID=$id;Name=$est_acc";

    print join "\t", @cols;
    print "\n";
  } elsif (/exonerate:\w+2genome/ && /\texon\t/) {
    $exon_id++;

    my @cols = split /\t/, $_;
    $cols[1] =~ s/chromosome:AgamP3:(\w+):.+/$1/; # if needed
    $cols[8] = "ID=$exon_id;Parent=$id";

    print join "\t", @cols;
    print "\n";
  }
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

