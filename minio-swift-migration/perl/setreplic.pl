# setreplic - Sets collection for migration
# Properties:
#     $ARGV[0] = file path to properties file
# Input:
#     <sysin> User requests
# Output:
#     modifications to MySQL tables to support specific collection
#     migration.
#**********************************************************************
use DBI;
use POSIX qw(strftime);

if (@ARGV < 1) {
   print "database prop name required";
}

# Initialize
my $dbprop = $ARGV[0];
my ($dbh, %db) = getDB($dbprop);
my $logfile = setLog(%db);

# Get mnemonic for new collection to migrate
print "Enter migrate collection mnemonic: ";
my $collectName = <STDIN>;
chomp $collectName;
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
$created = getCreated($dbh, $collid, $nodeid);
if (length($created) > 0) {
    logerr("FAIL: collection-node entry already exists for \"$collectName\" -  inv_collection_id=$collid; inv_node_id=$nodeid; created: $created\n");
}
my $insertCnt = setInsertCollectionNode($dbh, $collid, $nodeid);
my $updateCnt = setReplicatedNull($dbh, $collid);
my $msg = "SUCCESS Add: "
	. " - mnemonic=$collectName"
	. " - insert count=$insertCnt" 
	. " - update count=$updateCnt"
	. "\n";
print $msg;
logit($msg);

exit 0;

# Get MySQL database for this processing
sub getDB {
    my ($file) = @_;
    open FILE1, "$file" or die;
    my %db;
    while (my $line=<FILE1>) {
       chomp($line);
       (my $word1,my $word2) = split (/=/, $line, 2);
		#print "w1-$word1 w2-$word2\n";
       $db{$word1} = $word2;
    }
    close FILE1;
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

# Does inv_collection_inv_nodes entry already exist
sub getCreated {
    my ($dbh, $collid, $nodeid)  = @_;
    my $sql = 
		"SELECT created "
		. "FROM inv_collections_inv_nodes AS cn "
		. "WHERE cn.inv_collection_id=$collid AND cn.inv_node_id=$nodeid";
		
	#print "sql=$sql\n";
    my $sth = $dbh->prepare($sql)
        or logerr("prepare statement failed: $dbh->errstr()");
    $sth->execute() or logerr("execution failed: $dbh->errstr()");
	#print $sth->rows . " rows found.\n";
    my $rowcnt = $sth->rows;
    if ($rowcnt == 0) {
        return '';
    }

    while (my $ref = $sth->fetchrow_hashref()) {
        my $val = $ref->{'created'};
        if (length($val) > 0) {
            return $val
        }
    }
    $sth->finish;
    return '';
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

# Insert inv_collections_inv_nodes entry for migrating content
sub setInsertCollectionNode {
    my ($dbh, $collid, $nodeid)  = @_;
    my $cnt=0;
    my $insertsql = "insert into inv_collections_inv_nodes "
		. "set inv_collection_id=$collid, inv_node_id= $nodeid";
		
    #print( "insertsql=$insertsql\n");
    #return $cnt;
    my $cnt = $dbh->do($insertsql);
    if ($cnt == 0) {
    logerr("insert failed: "
                    . " - sql=$sql "
                    . " - Error: $dbh->errstr()"
                    );
    }
    print "INSERT cnt=$cnt\n";
    return $cnt;
}

# Set inv_nodes_inv_objects.replicated to null
# for all prime entries for a specific collection
sub setReplicatedNull {
   my ($dbh, $collid)  = @_;
   my $cnt = 0;
   my $updatesql = 'update inv_nodes_inv_objects '
            . 'set replicated = null '
            . 'where id in ( '
            . 'select tempTab.tempId '
            . 'FROM ( '
            . '  select inv_nodes_inv_objects.id as tempId '
            . '	from inv_nodes_inv_objects, inv_collections_inv_nodes, inv_collections_inv_objects '
            . '	where inv_collections_inv_objects.inv_collection_id = inv_collections_inv_nodes.inv_collection_id '
            . '	and inv_nodes_inv_objects.inv_object_id = inv_collections_inv_objects.inv_object_id '
            . "	and inv_nodes_inv_objects.role = 'primary' "
            . "	and inv_collections_inv_nodes.inv_collection_id=$collid "
            . '	) AS tempTab '
            . ') ';
    #print("updatesql=$sql\n");
    #return $cnt;
    $cnt = $dbh->do($updatesql);
    if ($cnt == 0) {
        logerr("insert failed: "
                    . " - sql=$sql "
                    . " - Error: $dbh->errstr()"
                    );
    }
    print "UPDATE cnt=$cnt\n";
    return $cnt;
}

# setup logging
sub setLog {
	my (%prop) = @_;
	my $logdir = $prop{'logdir'}
		or die "logdir not found\n";
	my $logname = $prop{'logname'}
		or die "logname not found\n";
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
