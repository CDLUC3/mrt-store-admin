# resetreplic - reset replicated to force reprocessing file
# Properties:
#     $ARGV[0] = file path to properties file
# Input:
#     <sysin> User requests
# Output:
#     modifications to MySQL tables to reset a collection
# 	  migration
#**********************************************************************
use DBI;
use POSIX qw(strftime);
use URI::Escape;

my $addBaseSQL = 'from inv_nodes_inv_objects as NO,  '
            . 'inv_objects as o,  '
            . 'inv_collections_inv_nodes as cn, ' 
            . 'inv_collections_inv_objects as co  '
            . 'WHERE co.inv_collection_id=? '
			. 'and no.inv_object_id = co.inv_object_id ' 
            . 'and cn.inv_collection_id = co.inv_collection_id ' 
            . 'and no.inv_object_id=o.id ' 
            . "AND NO.role='primary' "
            . 'and not no.replicated is null '
            . 'and no.inv_object_id not IN ( ' 
            . 'select o.id ' 
            . 'from inv_objects as o, ' 
            . 'inv_nodes as n,  '
            . 'inv_nodes_inv_objects as NO '
            . "where no.role = 'secondary' "
            . 'and no.inv_object_id = o.id ' 
            . 'and no.inv_node_id = n.id '
            . 'AND n.id=NO.inv_node_id '
            . "AND n.description LIKE '%minio%'"
            . '); ' ;
if (@ARGV < 1) {
   print "database prop name required";
}

# Initialize
my $dbprop = $ARGV[0];

my ($dbh, %db) = getDB($dbprop);
my $basedir = $db{'basedir'}
		or die "basedir not found\n";
my ($indir, $outdir,$logdir) = setBase(%db);
my $logfile = setLog(%db);

# Get mnemonic for new collection to migrate
my $collectName;
if (@ARGV == 2) {
	$collectName =  $ARGV[1];
	print "Propert mnemonic supplied:$collectName\n";
} else {
	print "Enter add reset collection mnemonic: ";
	my $collectName = <STDIN>;
	chomp $collectName;
}
if (length($collectName) == 0) {
	logerr("FAIL: no collection mnemonic supplied\n");
}

my $nodesql = $db{'nodesql'}
    or logerr("'nodesql' property not found\n");
my $collsql = $db{'collsql'}
    or logerr("'collectionsql' property not found\n");
my $nodeid = getDBVal($dbh, $nodesql, 'nodeid', '%minio%');
if (length($nodeid) == 0) {
    logerr("nodeid not found\n");
}
my $collid = getDBVal($dbh, $collsql, 'collid', $collectName);
if (length($collid) == 0) {
    logerr("FAIL: Submitted collection mnemonic not found: $collectName\n");
}
#print "collid=$collid nodeid=$nodeid\n";
my $resetCnt = getResetCount($dbh, $collid);
if ($resetCnt == 0) {
	logerr("WARNING '$collectName' found but requires no resets\n");
}

print "***Add $resetCnt rows\n";
my $addCnt = addReplicated($dbh, $collid, $resetCnt);

my $msg = "Re-add "
    . " - mnemonic: $collectName"
    . " - count: $addCnt"
	. "\n";
print $msg;
logit($msg);

exit 0;

# Get MySQL database for this processing
sub getDB {
    my ($file) = @_;
    my %db = getProp($file);
    my $database=$db{'database'};
    my $hostname=$db{'hostname'};
    my $port=$db{'port'};
    my $user=$db{'user'};
    my $password=$db{'password'};
    my $dsn = "DBI:mysql:database=$database;host=$hostname;port=$port";
    my $dbh = DBI->connect($dsn, $user, $password)
		or die "Couldn't connect to database: " 
		. " - database: $database"
		. " - user: $user"
		. " - error: DBI->errstr";
    return($dbh, %db);
}


# Get MySQL database for this processing
sub getProp {
    my ($file) = @_;
    open FILE1, "$file" or die;
    my %prop;
    while (my $line=<FILE1>) {
       chomp($line);
       (my $word1,my $word2) = split (/=/, $line, 2);
		#print "w1-$word1 w2-$word2\n";
       $prop{$word1} = $word2;
    }
    close FILE1;
    return(%prop);
}

sub getDBVal {
    my ($dbh, $sql, $name, $insert)  = @_;
	#print "sql=$sql\n name=$name\n insert=$insert\n";
    my $sth = $dbh->prepare($sql)
        or die logit("prepare statement failed: $dbh->errstr()");
    $sth->execute($insert) or logerr("execution failed: $dbh->errstr()");
	#print $sth->rows . " rows found.\n";
    my $rowcnt = $sth->rows;
    if ($rowcnt == 0) {
        return '';
    }

    while (my $ref = $sth->fetchrow_hashref()) {
        my $val = $ref->{$name};
        if (length($val) > 0) {
            return $val
        }
    }
    $sth->finish;
    return '';
}

# Get count for reset
sub getResetCount {
    my ($dbh, $collid)  = @_;
    my $cnt=0;
    my $cntsql = 'select count(distinct o.ark) as total ' . $addBaseSQL;
    #print "CNTSQL: $cntsql\n\n";
    my $sth = $dbh->prepare($cntsql)
        or die "prepare statement failed: $dbh->errstr()";
    $sth->execute($collid) or die "execution failed: $dbh->errstr()";
#print $sth->rows . " rows found.\n";
	my $ref = $sth->fetchrow_hashref();
	my $rowcnt=$ref->{'total'};
    return $rowcnt;
}

# Set inv_nodes_inv_objects.replicated to null
# for all prime entries for a specific collection
sub addReplicated {
   my ($dbh, $collid, $resetCnt)  = @_;
   my $cnt = 0;
   my $addreplicatesql = 'select distinct o.ark as ark ' . $addBaseSQL;
            
	# Get list of arks
	my $sth = $dbh->prepare($addreplicatesql)
    or die "prepare statement failed: $dbh->errstr()";
	$sth->execute($collid) or die "execution failed: $dbh->errstr()";

    my $outCnt=0;
	# For each ark call shell to add content
	my @arks;
	while (my $ref = $sth->fetchrow_hashref()) {
		$outCnt++;
		my $ark=$ref->{'ark'};
		push(@arks, $ark);
	}
	$sth->finish;
	$path = buildAddFile(@arks);
    return $outCnt;
}

# setup logging
sub buildAddFile {
	my (@arks) = @_;

	my $fileName = $collectName;
	my $inpath = $indir . '/' . "$fileName.sh";
	open(my $in, '>', $inpath) 
		or die "Could not open file '$logpath' $!";
	my $arkcnt=0;
    foreach $ark (@arks)
	{		
		my $encArk = uri_escape($ark);
		my $curl = `$basedir/bldcurl.sh $encArk`;
		print $in "$curl";
		$arkcnt++;
	}
	close $in;
	return $inpath;
}

# setup logging
sub setBase {
    $indir = "$basedir/in";
    $outdir = "$basedir/out";
    $logdir = "$basedir/logs";
    
    unless(-e "$indir" or mkdir "$indir") {
        die "Unable to create $indir\n";
    }
    unless(-e "$outdir" or mkdir "$outdir") {
        die "Unable to create $outdir\n";
    }
    unless(-e "$logdir" or mkdir "$logdir") {
        die "Unable to create $logdir\n";
    }
    return ($indir, $outdir, $logdir)
}

# setup logging
sub setLog {
	my $logname = $db{'logname'}
		or die "tsk logname not found\n";
    
	my $logpath = $logdir . '/' . $logname;

	open(my $log, '>>', $logpath) 
		or die "Could not open file '$logpath' $!";
	return $log;
}

#Issue log entry
sub logit {
	my ($msg) = @_;
	my $date = strftime "%Y-%m-%d %H:%M:%S", localtime;
	my $outmsg = "[$date] $msg";
	print $logfile "$outmsg";
	return $msg;
}

#Issue log entry then die
sub logerr {
	my ($msg) = @_;
	die logit($msg);
}
