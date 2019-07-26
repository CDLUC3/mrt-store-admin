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
my $sql = $db{'sql'}
    or die "'sql' property not found\n";

# Get list of collections
my $sth = $dbh->prepare($sql)
    or die "prepare statement failed: $dbh->errstr()";
$sth->execute() or die "execution failed: $dbh->errstr()";
my $sqlcnt = $db{'sqlcnt'};
my $sqlinc =  $db{'sqlinc'};

# create report
my $title = $db{'title'}
    or die "'title' property not found\n";
print "\n$title\n";
print $sth->rows . " collections found.\n";
print "db: $db{'hostname'}\n";
print "................................................\n\n";

# For each collection get object and byte counts
while (my $ref = $sth->fetchrow_hashref()) {
    my $cid=$ref->{'cid'};
    my $mnemonic = $ref->{'mnemonic'};
    my $missmsg = '';
    if ($sqlcnt ne '') {
		my $cnt = testCount($cid, $sqlcnt);
		if ($cnt == 0) {
			next;
		}
		$missmsg = "- missing objects: $cnt";
		if ($sqlinc ne '') {
			$incomplete = getValueCollection($cid, $sqlinc);
			$missmsg = $missmsg . " - incomplete: $incomplete";
		}
    }
    print "\nCollection: $mnemonic $missmsg\n";
    getCollection($cid, $mnemonic);
}
$sth->finish;
printTotalReport($dbh, %db);
exit;

# Get qualified collection information
sub testCount {
    my ($cid, $sqlcnt) = @_;
    #print "cid=$cid - sqlcnt: $sqlcnt\n\n";
    my $sth = $dbh->prepare($sqlcnt)
        or die "prepare statement failed: $dbh->errstr()";
    $sth->execute("$cid","$cid") or die "execution failed: $dbh->errstr()";
    my $ref = $sth->fetchrow_hashref();
    my $rowcnt=$ref->{'cnt'};
    #print "**CID: $cid -- cnt: $rowcnt\n";
    $sth->finish;
	return $rowcnt;
}

# Get qualified collection information
sub getCollection {
    my ($cid, $mnemonic) = @_;
 
    my $sql = 'SELECT c.mnemonic as mnemonic, c.id, n.NUMBER as nodenum, NO.role as role, COUNT(distinct co.inv_object_id) as objectcnt, sum(f.billable_size) as bytes '
    . 'FROM inv_collections AS c, '
    . 'inv_collections_inv_objects AS co, '
    . 'inv_nodes_inv_objects AS NO, '
    . 'inv_files AS f, '
    . 'inv_nodes AS n '
    . 'WHERE c.id = ? '
    . 'AND c.id=co.inv_collection_id '
    . 'AND co.inv_object_id=NO.inv_object_id '
    . 'AND NO.inv_node_id=n.id '
    . 'AND f.inv_object_id=NO.inv_object_id '
    . 'AND NOT mnemonic IS null '
    . 'GROUP BY c.id, NO.inv_node_id, NO.role;'; 

    my $sth = $dbh->prepare($sql)
        or die "prepare statement failed: $dbh->errstr()";
    $sth->execute("$cid") or die "execution failed: $dbh->errstr()";
    my $rowcnt = $sth->rows;
    if ($rowcnt == 0) {
        return;
    }

# print collection information
    while (my $ref = $sth->fetchrow_hashref()) {
        my $nodenum = $ref->{'nodenum'};
        my $objectcnt = $ref->{'objectcnt'};
        my $bytes = $ref->{'bytes'};
        my $role = $ref->{'role'};
        my $row = "$role node=$nodenum objects=$objectcnt bytes=$bytes";
        printf("-  role=%-9s node=%d objects=%d bytes=%d\n",$role,$nodenum, $objectcnt, $bytes);
    }
    $sth->finish;
    return;
}

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


sub getValueCollection {
	my ($cid, $sql) = @_;

	# Get list of collections
	my $stot = $dbh->prepare($sql)
		or die "prepare statement failed: $dbh->errstr()";
	$stot->execute($cid) or die "execution failed: $dbh->errstr()";
	my $ref = $stot->fetchrow_hashref();
    my $total=$ref->{'total'};
    $stot->finish;
    return $total;
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
			$totobjmigr, $totobjmigr, $perobj);
	printf("Bytes:    Migrated=%d  - Total=%d - %8.4f\%\n",
			$totbytemigr, $totbyteall, $perbyte);
	print "#" x 70 . "\n";
	return;
}
