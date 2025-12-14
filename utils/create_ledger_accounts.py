import re

insert_map = {}
i=0
with open(fname, 'r') as file:
    for line in file:
        insert_map[i + 1] = line
        i += 1
