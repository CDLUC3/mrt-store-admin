mnemonic='accum'
DATE=`date '+%Y%m%d-%H%M%S'`
cd in
./${mnemonic}.sh > ../out/out-${mnemonic}.$DATE.txt &
echo Submitted: ./${mnemonic}.sh
echo "> tail -f ../out/out-${mnemonic}.$DATE.txt"
tail -f ../out/out-${mnemonic}.$DATE.txt
