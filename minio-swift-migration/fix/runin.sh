mnemonic=$1
DATE=`date '+%Y%m%d-%H%M%S'`
chmod 755 ./in/${mnemonic}.sh
./in/${mnemonic}.sh > ./out/out-${mnemonic}.${DATE}.txt &
