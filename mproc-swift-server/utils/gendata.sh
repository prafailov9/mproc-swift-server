#!/bin/bash

# Define the file to which you want to append
output_file="sss.txt"

# Loop from 1 to 1000
for i in $(seq -w 1 1000)
do
   # Append the formatted string to the file
   printf ", mprocswift%s\n" "$i" >> "$output_file"
done
