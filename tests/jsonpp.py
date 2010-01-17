#! /usr/bin/env python
import sys
import json

file_name = sys.argv[1]
data = json.loads(file(file_name).read())
data = json.dumps(data, sort_keys=True, indent=4)

f = file(file_name, 'wt')
f.write(data)
f.close()
