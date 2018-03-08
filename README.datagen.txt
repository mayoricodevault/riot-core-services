

./dgale.sh -ic 0 -n 6 -r 2 -s 0 -c | ./alexml.sh | ./alepost.sh

cat data.txt | ./alexml.sh | ./alepost.sh

cat data.txt | ./alexml.sh -h file1.xml -b file2.xml | ./alepost.sh




