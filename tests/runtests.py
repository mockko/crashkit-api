#! /usr/bin/env python
import unittest
import pexpect
import urllib2
import os
import sys
import json
import re
import difflib

CRASHKIT_HOST = "localhost:5005"
AUX_PORT = 8236

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PYTHON_DIR = os.path.join(BASE_DIR, 'python')
PHP_DIR = os.path.join(BASE_DIR, 'php')
ENV = {'CRASHKIT_HOST': CRASHKIT_HOST}
REPORTS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'expected-reports')

class CrashKitClientTestCase(unittest.TestCase):
  
  def assertMatches(self, regexp, text):
    self.assertTrue(re.match(regexp, text), "Text does not match RE %s:\n%s\n" % (regexp, text))
  
  def assertTextEqual(self, expected, actual):
    if expected != actual:
      self.assertTrue(False, "Not equal:\n" + "\n".join([line for line in difflib.unified_diff(expected.split("\n"), actual.split("\n"), fromfile="expected.json", tofile="actual.json")]))
    else:
      self.assertTrue(True)
    
  def assertJsonEqual(self, expected, actual):
    self.assertTextEqual(json.dumps(expected, sort_keys=True, indent=4), json.dumps(actual, sort_keys=True, indent=4))
  
  pass
  
def normalize_report(report):
  report = report[:]
  for occur in report:
    if 'client_version' in occur:
      del occur['client_version']
  
    for exc in occur.get('exceptions', []):
      for loc in exc.get('locations', []):
        if 'file' in loc:
          loc['file'] = os.path.basename(loc['file'])
  return report
  
class PythonClientTestCase(CrashKitClientTestCase):
  
  def test_regular_python(self):
    print "testing command-line python..."
    child = pexpect.spawn('python', [os.path.join(PYTHON_DIR, 'sample.py')], env=ENV)
    child.logfile = sys.stdout
    child.expect(pexpect.EOF)
    
    body = urllib2.urlopen('http://%s/test/products/py/last-posted-report' % CRASHKIT_HOST).read()
    body = json.loads(body)
    
    self.assertEqual(1, len(body))
    self.assertMatches('{{ver}}|[0-9.]+', body[0].get('client_version', ''))

    body = normalize_report(body)
    expected = normalize_report(json.loads(file(os.path.join(REPORTS_DIR, 'python.json')).read()))
    self.assertJsonEqual(expected, body)
    
  def test_django(self):
    print "starting Django..."
    child = pexpect.spawn('python', [os.path.join(PYTHON_DIR, 'crashkit_django', 'manage.py'),
        'runserver', '%d' % AUX_PORT], env=ENV)
    child.expect('Quit the server with CONTROL-C')
    print "testing Django..."

    try:
      try:
        urllib2.urlopen('http://localhost:%d/polls/1/vote/?foo=bar&boz=biz' % AUX_PORT).read()
        self.assertTrue(False, "The request should have failed")
      except urllib2.HTTPError, e:
        self.assertEquals(500, e.code)
        body = e.read()
        self.assertTrue(body.find("<title>ZeroDivisionError at /polls/1/vote/</title>") >= 0,
          "Unexpected title of Django app error page")
    
      body = urllib2.urlopen('http://%s/test/products/django/last-posted-report' % CRASHKIT_HOST).read()
      body = json.loads(body)
    
      body = normalize_report(body)
      expected = normalize_report(json.loads(file(os.path.join(REPORTS_DIR, 'django.json')).read()))
      self.assertJsonEqual(expected, body)
    finally:
      print "shutting down Django..."
      child.sendintr()
      child.expect(pexpect.EOF)

  
class PhpClientTestCase(CrashKitClientTestCase):
  
  def test_command_line_php(self):
    print "testing command-line php..."
    child = pexpect.spawn('php', [os.path.join(PHP_DIR, 'crashkit-demo.php')], env=ENV)
    body = child.read()
    child.expect(pexpect.EOF)
    
    self.assertMatches(".*<title>Server error</title>.*", body)
    
    body = urllib2.urlopen('http://%s/test/products/php/last-posted-report' % CRASHKIT_HOST).read()
    body = json.loads(body)
    
    self.assertEqual(3, len(body))
    self.assertMatches('{{ver}}|[0-9.]+', body[0].get('client_version', ''))

    body = normalize_report(body)
    expected = normalize_report(json.loads(file(os.path.join(REPORTS_DIR, 'php.json')).read()))
    self.assertJsonEqual(expected, body)


if __name__ == '__main__':
  unittest.main()
