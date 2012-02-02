#!/usr/bin/perl -w
#
# creates a tsv annotation table from TGI_cl cluster output
#
# usage: ./tgi-cluster-ace-to-tsv.pl ACE_file > cluster_annot.tsv
#
#end

use Getopt::Long;


GetOptions(
	   "help"=>\$help,
	  );


usage() if ($help);


print join("\t",
	   '#cluster',
	   'EST accession',
	   'EST description',
	  )."\n";

my %cluster2ests;    # cluster_id => [ array of ids ]
my %est2description; # est_id => description

my ($cl_id, $est_id);

while (<>) {
  if (/^CO\s+(\S+)/) {
    $cl_id = $1;
  } elsif ($cl_id && /^AF\s+(\S+)/) {
    push @{$cluster2ests{$cl_id}}, $1;
  } elsif ($cl_id && /^RD\s+(\S+)/) {
    $est_id = $1;
  } elsif ($est_id && /^DS\s+(.+)/) {
    $est2description{$est_id} = $1;
  }
}

foreach $cl_id (keys %cluster2ests) {
  foreach $est_acc (@{$cluster2ests{$cl_id}}) {
    print "$cl_id\t$est_acc\t$est2description{$est_acc}\n";
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
