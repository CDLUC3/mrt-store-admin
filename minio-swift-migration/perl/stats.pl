# report-generic - Used to build report of object and byt cound by collection
# Properties:
#     $ARGV[0] = file path to properties file
# Output:
#     sysout report
#**********************************************************************
use DBI;
if (@ARGV < 2) {
   die "database prop name required";
}
my $propfrom = $ARGV[0];
my $propto= $ARGV[1];
my $inbase = '../out/progress';

# Get MySQL handler
my $fromFile = "${inbase}/${propfrom}.prop";
print "fromFile=$fromFile\n";
my $toFile = "${inbase}/${propto}.prop";
print "toFile=$toFile\n";

my %pfrom = getProp($fromFile);
my %pto = getProp($toFile);
getStats();

# Open connection to database
sub getProp {
    my ($file) = @_;
    open FILE1, "$file" or die "Could not open: $file";
    my %prop;
    print "\n\n***file=$file\n";
    while (my $line=<FILE1>) {
       chomp($line);
       (my $word1,my $word2) = split (/=/, $line, 2);
       $prop{$word1} = $word2;
       print "$word1=$word2\n";
    }
    close FILE1;
    return %prop;
}

sub getStats {
	print "\nStats\n";
	my $fromNormsec = $pfrom{"normsec"};
	my $fromTotobjmigr = $pfrom{"totobjmigr"};
	my $fromTotobjall = $pfrom{"totobjall"};
	my $fromTotbytemigr = $pfrom{"totbytemigr"};
	my $fromTotbyteall = $pfrom{"totbyteall"};
	my $fromPerbyte = $pfrom{"perbyte"};
	my $fromPerobj = $pfrom{"perobj"};
	my $fromDatetime = $pfrom{"datetime"};
	
	my $toNormsec = $pto{"normsec"};
	my $toTotobjmigr = $pto{"totobjmigr"};
	my $toTotobjall = $pto{"totobjall"};
	my $toTotbytemigr = $pto{"totbytemigr"};
	my $toTotbyteall = $pto{"totbyteall"};
	my $toPerbyte = $pto{"perbyte"};
	my $toPerobj = $pto{"perobj"};
	my $toDatetime = $pto{"datetime"};
	#print "*** fromNormsec=$fromNormsec - toNormsec=$toNormsec\n";
	my $diffNormsec = $toNormsec - $fromNormsec;
	my $diffTotobjmigr = $toTotobjmigr - $fromTotobjmigr;
	my $diffTotobjall = $toTotobjall - $fromTotobjall;
	my $diffTotbytemigr = $toTotbytemigr - $fromTotbytemigr;
	my $diffTotbyteall = $toTotbyteall - $fromTotbyteall;
	my $objleft = $toTotobjall - $toTotobjmigr;
	my $bytesleft = $toTotbyteall - $toTotbytemigr;
	my $bytespersec = $diffTotbytemigr / $diffNormsec;
	my $bytesec = $bytesleft / $bytespersec;
	my $bytedays = $bytesec/(24*3600);
	
	my $adjbytesmigr = $diffTotbytemigr-$diffTotbyteall;
	my $adjbytes = $bytesleft/$adjbytesmigr;
	my $adjbytessec = $adjbytes * $diffNormsec;
	my $adjbytesdays = $adjbytessec/(24*3600);
	
	print "***Adjusted Bytes\n"
		. " - diffTotbytemigr=$diffTotbytemigr\n"
		. " - adjbytesmigr=$adjbytesmigr\n"
		. " - adjbytes=$adjbytes\n"
		. " - adjbytessec=$adjbytessec\n"
		. " - adjbytesdays=$adjbytesdays\n"
		;
	
	my $adjobjmigr = $diffTotobjmigr-$diffTotobjall;
	my $adjobj = $objleft/$adjobjmigr;
	my $adjobjsec = $adjobj * $diffNormsec;
	my $adjobjdays = $adjobjsec/(24*3600);
	#**********************
	my $noaddbytesleft = $fromTotbyteall-$toTotbytemigr;
	my $noaddbytes = $noaddbytesleft/$diffTotbytemigr;
	my $noaddbytessec = $noaddbytes * $diffNormsec;
	my $noaddbytesdays = $noaddbytessec/(24*3600);
	
	print "***No add Bytes\n"
		. " - noaddbytesleft=$noaddbytesleft\n"
		. " - diffTotbytemigr=$diffTotbytemigr\n"
		. " - noaddbytessec=$noaddbytessec\n"
		. " - noaddbytesdays=$noaddbytesdays\n"
		. " - adjbytesdays=$adjbytesdays\n"
		;
	#**********************
	my $adjobjmigr = $diffTotobjmigr-$diffTotobjall;
	my $adjobj = $objleft/$adjobjmigr;
	my $adjobjsec = $adjobj * $diffNormsec;
	my $adjobjdays = $adjobjsec/(24*3600);
	
	print "***Adjusted Objects\n"
		. " - diffTotobjmigr=$diffTotobjmigr\n"
		. " - adjobjmigr=$adjobjmigr\n"
		. " - adjobj=$adjobj\n"
		. " - adjobjsec=$adjobjsec\n"
		. " - adjobjdays=$adjobjdays\n"
		;
		
	my $objleft = $toTotobjall - $toTotobjmigr;
	my $objpersec = $diffTotobjmigr / $diffNormsec;
	my $objsec = $objleft / $objpersec;
	my $objdays = $objsec/(24*3600);
	
	print "diffNormsec=$diffNormsec\n";
	print "diffTotobjmigr=$diffTotobjmigr\n";
	print "diffTotobjall=$diffTotobjall\n";
	print "diffTotbytemigr=$diffTotbytemigr\n";
	print "diffTotbyteall=$diffTotbyteall\n";
	
	print "$bytesec = $bytesleft / $bytespersec\n";
	print "$bytedays bytedays = $bytesec bytesec/(24*3600)\n";
	
	print "$adjbytesdays adjbytesdays = $adjbytessec adjbytessec/(24*3600)\n";
	
	print "$objsec = $objleft / $objpersec\n";
	print "$objdays objdays = $objsec objsec/(24*3600)\n";
	
	my $testday=$bytesleft/($bytespersec*24*3600);
	
	print "&&&|$toDatetime|$toPerobj|$toPerbyte|$adjobjdays|$adjbytesdays|$bytespersec\n";
	
	print "@@@|$objleft|$bytesleft|$adjobjdays|$adjbytesdays|$bytespersec\n";
	
	print "+++|$adjobjdays|$adjbytesdays|$noaddbytesdays|$bytespersec|$bytesleft|$testday\n";
	
	print "===|$toDatetime|$diffTotobjall|$objleft|$diffTotobjmigr|$diffTotbyteall|$bytesleft|$diffTotbytemigr\n";
	

}
