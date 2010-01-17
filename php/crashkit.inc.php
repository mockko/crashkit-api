<?php
# Include this file to use CrashKit for bug reporting in your application.
# Visit http://crashkitapp.appspot.com/ for details.

# Copyright (c) 2009 Andrey Tarantsov, YourSway LLC
# 
# Permission to use, copy, modify, and/or distribute this software for any
# purpose with or without fee is hereby granted, provided that the above
# copyright notice and this permission notice appear in all copies.
# 
# THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
# WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
# ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
# WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
# ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
# OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

define('CRASHKIT_VERSION', '{{ver}}');

if(!function_exists('curl_init')) {
  trigger_error('You need to have CURL extension installed to use CrashKit.', E_USER_ERROR);
  return;
}

if (count(debug_backtrace(false)) == 0) {
  die(
    '<title>Welcome to CrashKit</title>' .
    '<div style="width: 600px; margin: 50px auto 0px auto; font: 14px Verdana, sans-serif;">' .
    '<p><b>Welcome to CrashKit</b></p>' .
    '<p>This file should be included into your application, not visited on its own.</p>' .
    '<p>Please see <a href="http://crashkitapp.appspot.com/">crashkitapp.appspot.com</a> for more information.</p>'
  );
}

if (!defined('CRASHKIT_ACCOUNT_AND_PRODUCT')) {
  trigger_error("You must define('CRASHKIT_ACCOUNT_AND_PRODUCT', 'account/product') before requiring crashkit.php.", E_USER_ERROR);
  return;
}
if(count(explode("/", CRASHKIT_ACCOUNT_AND_PRODUCT)) != 2) {
  trigger_error("CRASHKIT_ACCOUNT_AND_PRODUCT must have 'account/product' format.", E_USER_ERROR);
  return;
}
$_crashkit_account_and_product = explode("/", CRASHKIT_ACCOUNT_AND_PRODUCT);
define('CRASHKIT_ACCOUNT', $_crashkit_account_and_product[0]);
define('CRASHKIT_PRODUCT', $_crashkit_account_and_product[1]);
define('CRASHKIT_ROLE_COOKIE_NAME', 'crk_'.CRASHKIT_ACCOUNT.'_'.CRASHKIT_PRODUCT.'_role');

if(!defined('CRASHKIT_ROLE')) {
  $role = 'customer';
  if (isset($_COOKIE[CRASHKIT_ROLE_COOKIE_NAME]))
    $role = $_COOKIE[CRASHKIT_ROLE_COOKIE_NAME];
  define('CRASHKIT_ROLE', $role);
}

if (isset($_REQUEST['crashkitadmin'])) {
  $pass = $_REQUEST['crashkitadmin'];
  $role = CRASHKIT_ROLE;
  if ($pass != CRASHKIT_ADMIN_PASSWORD) {
    die(
      '<title>CrashKit access denied</title>' .
      '<div style="width: 600px; margin: 50px auto 0px auto; font: 14px Verdana, sans-serif;">' .
      '<p><b>CrashKit access denied</b></p>' .
      '<p>You have tried to access CrashKit admin page, but you have provided a wrong password.</p>'
    );
  }
  $action = (isset($_REQUEST['crashkitaction']) ? $_REQUEST['crashkitaction'] : 'index');
  if ($action == 'index') {
    $account = CRASHKIT_ACCOUNT;
    $product = CRASHKIT_PRODUCT;
    echo <<<EOT
      <title>CrashKit admin panel</title>
      <div style="width: 600px; margin: 50px auto 0px auto; font: 14px Verdana, sans-serif;">
        <p><b>CrashKit admin panel for $account/$product</b></p>
        <p>See the <a target="_new" href="http://crashkitapp.appspot.com/$account/products/$product/">list of bugs</a> on CrashKit server.</p>
        <p><b>Developer or tester?</b></p>
        <p>Your current role is <b>$role</b>.</p>
        <form method="POST">
          <input type="hidden" name="crashkitadmin" value="$pass" />
          <input type="hidden" name="crashkitaction" value="setrole" />
          <input type="hidden" name="role" value="disabled" />
          <input type="submit" value="Disable bug reporting" /> &mdash; set a disabling cookie. Bugs are not logged on CrashKit servers. Instead, developers get a detailed report in their web browser.
        </form>
        <form method="POST">
          <input type="hidden" name="crashkitadmin" value="$pass" />
          <input type="hidden" name="crashkitaction" value="setrole" />
          <input type="hidden" name="role" value="developer" />
          <input type="submit" value="I am a developer" /> &mdash; set a developer cookie. Bugs from developer machines are logged on CrashKit servers under a separate section. Additionally, developers get a detailed report in their web browser.
        </form>
        <form method="POST">
          <input type="hidden" name="crashkitadmin" value="$pass" />
          <input type="hidden" name="crashkitaction" value="setrole" />
          <input type="hidden" name="role" value="tester" />
          <input type="submit" value="I am a tester" /> &mdash; set a tester cookie. Bugs from <em>tester</em> machines are logged on CrashKit server under a separate section. Additionally, tester get a detailed report in their web browsers.
        </form>
        <form method="POST">
          <input type="hidden" name="crashkitadmin" value="$pass" />
          <input type="hidden" name="crashkitaction" value="setrole" />
          <input type="hidden" name="role" value="customer" />
          <input type="submit" value="I am a regular visitor" /> &mdash; remove all cookies.
        </form>
      </div>
EOT;
  } else if ($action == 'setrole') {
    $role = $_POST['role'];
    if ($role == 'customer')
      $expiry = time()-60*60*24*365*10; # 10 years ago
    else
      $expiry = time()+60*60*24*365*10; # 10 years from now
    setcookie(CRASHKIT_ROLE_COOKIE_NAME, $role, $expiry, '/');
    header('Location: http://'.$_SERVER['HTTP_HOST'].$_SERVER['PHP_SELF']."?crashkitadmin=$pass");
  }
  exit();
}
if (!defined('CRASHKIT_DIE_MESSAGE'))
  define('CRASHKIT_DIE_MESSAGE', '<title>Server error</title><div style="width: 600px; margin: 50px auto 0px auto; font: 14px Verdana, sans-serif;"><b style="color: red;">Server error</b><p>Sorry, we have failed to process your request now. Please try again later.<p>Our developers have just been notified about this error.</div>');
$GLOBALS['crashkit_error_queue'] = array();
register_shutdown_function('crashkit_send_errors');
set_error_handler('crashkit_error_handler');

function crashkit_is_disabled() {
  return CRASHKIT_ROLE == 'disabled';
}

function crashkit_is_developer() {
  return CRASHKIT_ROLE == 'developer' || CRASHKIT_ROLE == 'disabled';
}

function crashkit_is_tester() {
  return CRASHKIT_ROLE == 'tester';
}

function crashkit_is_developer_or_tester() {
  return crashkit_is_developer() || crashkit_is_tester();
}

function crashkit_error_handler($errno, $errmsg, $filename, $linenum, $vars) {
  if (error_reporting() == 0)
    return;
    
  $errortype = array (
    E_ERROR              => 'Error',
    E_WARNING            => 'Warning',
    E_PARSE              => 'Parsing Error',
    E_NOTICE             => 'Notice',
    E_CORE_ERROR         => 'Core Error',
    E_CORE_WARNING       => 'Core Warning',
    E_COMPILE_ERROR      => 'Compile Error',
    E_COMPILE_WARNING    => 'Compile Warning',
    E_USER_ERROR         => 'User Error',
    E_USER_WARNING       => 'User Warning',
    E_USER_NOTICE        => 'User Notice',
    E_STRICT             => 'Runtime Notice',
    E_RECOVERABLE_ERROR  => 'Catchable Fatal Error'
  );
    
  $user_errors = array(E_USER_ERROR, E_USER_WARNING, E_USER_NOTICE);
  
  $include_path = explode(PATH_SEPARATOR, get_include_path());
  foreach($include_path as &$dir) {
    if ($dir == ".")
      $dir = getcwd();
    $trailer = $dir[strlen($dir)-1];
    if ($trailer != DIRECTORY_SEPARATOR && $trailer != '/') {
      $dir .= DIRECTORY_SEPARATOR;
    }
  }
  
  $locations = array();
  $backtrace = debug_backtrace(false);
  if (is_null($backtrace[0]["file"]))
    array_shift($backtrace);
  else if ($backtrace[0]["function"] == "crashkit_error_handler")
    $backtrace[0]["function"] = null;
  foreach($backtrace as $location) {
    $file = $location["file"];
    $dir = dirname($file) . DIRECTORY_SEPARATOR;
    foreach ($include_path as $path)
      if (strpos($path, $dir) === 0) {
        $file = substr($file, strlen($path));
        break;
      }
    $locations[] = array(
      "file" => $file,
      "class" => $location["class"],
      "function" => $location["function"],
      "line" => $location["line"]
    );
  };
  
  $severe = in_array($errno, array(E_USER_ERROR, E_ERROR, E_COMPILE_ERROR, E_CORE_ERROR, E_RECOVERABLE_ERROR));
  
  $message = array(
    "exceptions" => array(array (
      "name" => $errortype[$errno],
      "message" => $errmsg,
      "locations" => $locations
    )),
    "data" => array(),
    "env" => array(
      "php_version" => phpversion()
    ),
    'role' => CRASHKIT_ROLE,
    "severity" => ($severe ? "major" : "normal"),
    "language" => "php",
    "client_version" => CRASHKIT_VERSION
  );
  
  if (crashkit_is_developer_or_tester()) {
    print "<div style='margin: 10px 0 0 0; padding: 0 0 5px 0; font: 14px Verdana, sans-serif; color: #000; background: #fff; border: 1px dotted #f00;'>";
    print "<p style='margin: 0 0 0 0; padding: 5px 5px;'><b style='color: #f00;'>".$errortype[$errno].":</b> ".$errmsg."</p>\n";
    print "<div style='font-size: 13px;'>";
    foreach($locations as $location) {
      $file  = $location['file'];
      $class = $location['class'];
      $func  = $location['function'];
      $line  = $location['line'];
      if ($class && $func)
        $f = " (in $class.$func)";
      else if ($func)
        $f = " (in $func)";
      else
        $f = "";
      print "<p style='margin: 0 0 0 20px;'>$file<span style='color: #777;'>:$line</span>$f</p>";
    }
    print "</div>";
    print "</div>";
  }
  if (!crashkit_is_disabled())
    $GLOBALS['crashkit_error_queue'][] = $message;
  if ($severe) {
    die(CRASHKIT_DIE_MESSAGE);
  }
}

function crashkit_send_errors() {
  $queue = $GLOBALS['crashkit_error_queue'];
  if (count($queue) == 0)
    return;
  $payload = json_encode($queue);
  
  $account_name = CRASHKIT_ACCOUNT;
  $product_name = CRASHKIT_PRODUCT;
  $host = "crashkitapp.appspot.com";
  if (isset($_ENV['CRASHKIT_HOST']))
    $host = $_ENV['CRASHKIT_HOST'];
  
  $ch = curl_init();
  curl_setopt($ch, CURLOPT_URL, "http://$host/$account_name/products/$product_name/post-report/0/0");
  curl_setopt($ch, CURLOPT_POST, 1);
  curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
  curl_setopt($ch, CURLOPT_HTTPHEADERS, array('Content-Type: application/json'));
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
  curl_exec($ch);
}
?>
