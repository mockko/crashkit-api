
# normally, you would do this:
# import crashkit

from crashkit_django import crashkit
import os

DEBUG = False
crashkit.initialize_crashkit('test', 'py',
  app_dirs=[os.path.dirname(__file__)],       # all other directories will be treated as external libs
  app_dir_exclusions=['django', 'coolstuff'], # some subdirs contain libraries too
  role=('disabled' if DEBUG else 'customer')  # disable CrashKit while debugging
  # this role is the default one, but you can override it using:
  #  environment variable:    PY_CRASHKIT_ROLE
  #  contents of file:        ~/py.role  or  ~/.py.role
  # valid roles are 'disabled' and 'customer' ('tester' will be supported soon)
)

import re

def zoo(self, a, b):
  return a(b)

def poo(a, b):
  return Boo().tiger(a, b)
  
class Boo:
  
  def ku(self, a):
    a.match("#$%^&*([[[[[[)", "a")
    
  @staticmethod
  def smoo(a, b):
    c = Boo()
    c.oop = poo
    return c.oop(a, b)
  
  @classmethod
  def cmoo(klass, a, b):
    return Boo.smoo(a, b)
    
Boo.tiger = zoo


def mpoo(a, b):
  return Moo().tiger(a, b)
  
class Moo(object):
  
  def moo(self, a):
    Boo.cmoo(Boo().ku, a)
    
  @staticmethod
  def smoo(a, b):
    c = Moo()
    c.oop = mpoo
    return c.oop(a, b)
  
  @classmethod
  def cmoo(klass, a, b):
    return Moo.smoo(a, b)
    
Moo.tiger = zoo

def main():
  x = {}
  try:
    Moo.cmoo(Moo().moo, re)
  except Exception, e:
    crashkit.send_exception()
  
main()
