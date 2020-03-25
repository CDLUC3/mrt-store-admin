use URI::Escape;
encFile($ARGV[0], $ARGV[1]);

# Process Gen
sub encFile
{
   my($infile, $outfile) = @_;
   open (OUTFILE, ">>$outfile");
   open (INFILE, $infile) or die "Unable to find file:$infile";
   @fields = ();
   while (<INFILE>) {
        chomp($_);
        $objid = $_;
        #print "Delete:$objid\n";
        my $responseLine = callDelete($objid);
        print "$responseLine\n";
        print OUTFILE "$responseLine\n";
   }
   close (INFILE);
   close (OUTFILE);
}


# Call delete
sub callDelete
{
    my($objid) = @_;

    my $respReplic = deleteReplic($objid);

    my $respStore = deleteStore($objid);

    my $respInv = deleteInv($objid);

    my $respLocal = deleteLocal($objid);

    return "$objid $respReplic $respStore $respInv $respLocal";
}


# Call delete store
sub deleteStore 
{
    my($objid) = @_;
    my $enc = uri_escape($objid);
    $curl = './manStore.sh';
    $cmd = $curl . ' ' . "\"" . $enc . "\"";
    #print "CMD= $cmd\n";
    $resultMan = `$cmd`;
    #print "CMD: $cmd:\nRESULT:\n$resultMan\n";
    my $url = '';
    if($resultMan =~ m/<invman:manifestUrl>(.*)<\/invman:manifestUrl>/) {
        #print "Match:$1\n";
        $url = $1;
        $url =~ s/\/manifest\//\/content\//;
        #print "After:$url\n";
        if( length($url) > 0) {
            my $curlDel = './deleteStore.sh';
            my $cmdDel = $curlDel . ' ' . "\"" . $url . "\"";
            #print "cmdDel:\n$cmdDel\n";
            $resultDel = `$cmdDel`;
            #print "cmdDel:\n$cmdDel\nresultDel:\n$resultDel";
            my $xpos = index($resultDel, '<obj:totalActualSize>');
            my $zpos = index($resultDel, 'REQUESTED_ITEM_NOT_FOUND');
            #print "zpos=$zpos - xpos=$xpos\n";
            if ($zpos >= 0) {
                $retstr = "NONE";
            } elsif ($xpos >= 0) {
                $retstr = "OK";
            } else {
                $retstr = "FAIL";
            }
            return "STORE=$retstr";
        }
    } else {
        return "STORE=NONE";
    }
}

# Call delete inv
sub deleteInv
{
    my($objid) = @_;
    my $enc = uri_escape($objid);
    my $retstr = "";
    $curl = './deleteInv.sh';
    $cmd = $curl . ' ' . "\"" . $enc . "\"";
#print "CMD= $cmd\n";
    $result = `$cmd`;
    #print "CMD: $cmd:\nRESULT:\n$result\n";
    my $xpos = index($result, '<invd:deleteCount>');
    my $zpos = index($result, '<invd:deleteCount>0</invd:deleteCount>');
    if ($xpos < 0) {
        $retstr = "FAIL";
    } elsif ($zpos < 0) {
        $retstr = "OK";
    } else {
        $retstr = "NONE";
    }
    return "INV=$retstr";
}



# Call delete local
sub deleteLocal
{
    my($objid) = @_;
    my $enc = uri_escape($objid);
    my $retstr = "";
    $curl = './deleteLocal.sh';
    $cmd = $curl . ' ' . "\"" . $enc . "\"";
    #print "CMD= $cmd\n";
    $result = `$cmd`;
    #print "CMD: $cmd:\nRESULT:\n$result\n";
    my $xpos = index($result, '<invloc:deleteCnt>');
    my $zpos = index($result, '<invloc:deleteCnt>0</invloc:deleteCnt>');
    #print "xpos: $xpos - zpos: $zpos\n";
    if ($xpos < 0) {
        $retstr = "FAIL";
    } elsif ($zpos < 0) {
        $retstr = "OK";
    } else {
        $retstr = "NONE";
    }
    return "LOCAL=$retstr";
}

# Call delete replic secondary
sub deleteReplic
{
    my($objid) = @_;
    my $enc = uri_escape($objid);
    my $retstr = "";
    $curl = './deleteReplic.sh';
    $cmd = $curl . ' ' . "\"" . $enc . "\"";
#print "CMD= $cmd\n";
    $result = `$cmd`;
#    print "CMD: $cmd:\nRESULT:\n$result\n";
    my $xpos = index($result, '<repdel:deleteCount>');
    my $zpos = index($result, '<repdel:deleteCount>0</repdel:deleteCount>');
    if ($xpos < 0) {
        $retstr = "FAIL";
    } elsif ($zpos < 0) {
        $retstr = "OK";
    } else {
        $retstr = "NONE";
    }
    return "REPLIC=$retstr";
}
