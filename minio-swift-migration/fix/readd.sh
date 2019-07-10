dbtype='prd';
mnemonic=$1;
perl ../perl/readd.pl ../conf/${dbtype}readd.prop $mnemonic
