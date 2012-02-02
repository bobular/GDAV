#!/usr/bin/perl -w

#
# WARNING: do not use this script!
#
# Use a proper BioPerl-based parser instead.
#
# Quickly written blast parsers like this one are very prone to errors.
#

use Getopt::Long;

my $min_aln_len = 20;

GetOptions(
	   "minalnlen|min_aln_len|min_alignment_length=i"=>\$min_aln_len,
	  );
print "Query\tSubject\t%Identity\te-val\tScore\tFrame\t%Coverage (subject)\tSubject length\tDescription\n";
# parse blastx output (crudely)

my ($query, $gothits, %hitinfo);

while (<>) {
  if (/^BLASTX/) {
    $query = undef;
    $gothits = 0;
    %hitinfo = ();
  }
  if (/Query= (\S+)/) {
    $query = $1;
  } elsif ($query && /^Sequences producing significant alignments/) {
    $gothits = 1;
  } elsif ($gothits) {
    # the 'not' conditions in the if/else conditions below
    # ensure that only the best hit is extracted
    if (!$hitinfo{acc} && /^\>(\S+)\s?(.*)/) {
      my ($acc, $desc) = ($1, $2);
      $hitinfo{acc} = $acc;
      $hitinfo{desc} = $desc;
    } elsif ($hitinfo{acc} && /^\s+Length\s+=\s+(\d+)/) {
      $hitinfo{len} = $1;
    } elsif ($hitinfo{acc} && $hitinfo{desc} && not exists $hitinfo{len}) {
      # we must be in subsequent lines of the hit description
      $hitinfo{desc} .= $_;
    } elsif ($hitinfo{acc} && !$hitinfo{score} && /^\s*Score\s+=\s+(\S+)\s+bits/) {
      $hitinfo{score} = $1;
      ($hitinfo{evalue}) = /Expect\S* = (\S+)/;
    } elsif ($hitinfo{acc} && !$hitinfo{pid} && /^\s*Identities\s+=\s+(\d+)\/(\d+)/) {
      $hitinfo{pid} = int(100*$1/$2);
      $hitinfo{alnlen} = $2;
    } elsif ($hitinfo{acc} && !$hitinfo{frame} && /^\s*Frame\s+=\s+(\S+)/) {
      $hitinfo{frame} = $1;

      # now we have all the hit info
      chomp($hitinfo{desc});
      $hitinfo{desc} =~ s/\s+/ /g;

      my $targetcoverage = sprintf "%d", 100*$hitinfo{alnlen}/$hitinfo{len};

      print "$query\t$hitinfo{acc}\t$hitinfo{pid}\t$hitinfo{evalue}\t$hitinfo{score}\t$hitinfo{frame}\t$targetcoverage\t$hitinfo{len}\t$hitinfo{desc}\n";
    }
  }
}

