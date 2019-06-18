#!/usr/bin/perl
# mailconfig - format and send email
# Properties:
#     $ARGV[0] = file path to properties file
#     $ARGV[1] = Name of attachment
# Output:
#     SMTP email - with report attachment
#**********************************************************************
use MIME::Lite;
if (@ARGV < 2) {
   die "needed arguments not provided";
}

my $configFile = $ARGV[0];
print "configFile=$configFile\n";

my $reportName = $ARGV[1];
my $attachName="$reportName.txt";
my $subject = "Report: $reportName";
 
my %mail = getMailProp($configFile);
my $attachDir = $mail{'attachDir'};
my $attach = $attachDir . $attachName;

my $msg = MIME::Lite->new(
                 From     => $mail{'from'},
                 To       => $mail{'to'},
#                Cc       => $mail{'cc'},
                 Subject  => $subject,
                 Type     => 'multipart/mixed'
                 );
                 
# Add your text message.
$msg->attach(Type         => 'text',
             Data         => $mail{'message'}
             );
            
# Specify your file as attachement.
$msg->attach(Type         => 'text/plain',
             Path         => $attach,
             Filename     => $attachName,
             Disposition  => 'attachment'
            );       
$msg->send('smtp', $mail{'smtp'});
print "Email Sent Successfully\n";

# Get properties for this mail
sub getMailProp {
    my ($file) = @_;
    open FILE1, "$file" or die;
    my %mail;
    while (my $line=<FILE1>) {
       chomp($line);
       (my $word1,my $word2) = split /=/, $line;
       $mail{$word1} = $word2;
    }
    close FILE1;
    return(%mail)
}

