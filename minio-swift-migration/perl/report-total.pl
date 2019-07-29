# report-generic - Used to build report of object and byt cound by collection
# Properties:
#     $ARGV[0] = file path to properties file
# Output:
#     sysout report
#**********************************************************************
use DBI;
if (@ARGV < 1) {
   die "database prop name required";
}
my $dbprop = $ARGV[0];

# Get MySQL handler
my ($dbh, %db) = getDB($dbprop);
my $title = $db{'title'}
    or die "'title' property not found\n";
print "\n$title\n";

print "................................................\n\n";

printTotalReport($dbh, %db);

exit;


# Open connection to database
sub getDB {
    my ($file) = @_;
    open FILE1, "$file" or die "Could not open: $file";
    my %db;
    while (my $line=<FILE1>) {
       chomp($line);
       (my $word1,my $word2) = split (/=/, $line, 2);
       $db{$word1} = $word2;
    }
    close FILE1;
    my $database=$db{'database'};
    my $hostname=$db{'hostname'};
    my $port=$db{'port'};
    my $user=$db{'user'};
    my $password=$db{'password'};
    my $dsn = "DBI:mysql:database=$database;host=$hostname;port=$port";
    my $dbh = DBI->connect($dsn, $user, $password);
    return($dbh, %db);
}

sub getTotals {
	my ($dbh, %db) = @_;
	my $sqlobjall = $db{'sqlobjall'}
		or die "'sqlobjall' property not found\n";
    my $totobjall = getValue($dbh, $sqlobjall);
 #   print "totobjall=$totobjall\n";
    
	my $sqlobjmigr = $db{'sqlobjmigr'}
		or die "'sqlobjmigr' property not found\n";
    my $totobjmigr = getValue($dbh, $sqlobjmigr);
#    print "totobjmigr=$totobjmigr\n";
    
	my $sqlbyteall = $db{'sqlbyteall'}
		or die "'sqlbyteall' property not found\n";
    my $totbyteall = getValue($dbh, $sqlbyteall);
#    print "totbyteall=$totbyteall\n";
    
	my $sqlbytemigr = $db{'sqlbytemigr'}
		or die "'sqlbytemigr' property not found\n";
    my $totbytemigr = getValue($dbh, $sqlbytemigr);
#    print "totbytemigr=$totbytemigr\n";
    
    return ($totobjall, $totobjmigr, $totbyteall, $totbytemigr);
    
}

sub getValue {
	my ($dbh, $sql) = @_;

	# Get list of collections
	my $stot = $dbh->prepare($sql)
		or die "prepare statement failed: $dbh->errstr()";
	$stot->execute() or die "execution failed: $dbh->errstr()";
	my $ref = $stot->fetchrow_hashref();
#	my %h = %$ref;
#   while (my ($k,$v)=each %h){print "$k $v\n"}
    my $total=$ref->{'total'};
    $stot->finish;
    return $total;
}

sub printTotalReport {
	my ($dbh, %db) = @_;
	my $sqlbyteall = $db{'sqlbyteall'}
		or return;
	my ($totobjall, $totobjmigr, $totbyteall, $totbytemigr) = getTotals($dbh, %db);

	print "\n";
	print "#" x 70 . "\n";
	print "TOTALS\n\n";
	my $perobj=($totobjmigr/$totobjall) * 100;
	my $perbyte=($totbytemigr/$totbyteall) * 100;
	printf("Objects:  Migrated=%d  - Total=%d - %8.4f\%\n",
			$totobjmigr, $totobjall, $perobj);
	printf("Bytes:    Migrated=%d  - Total=%d - %8.4f\%\n",
			$totbytemigr, $totbyteall, $perbyte);
	print "#" x 70 . "\n";
	return;
}
