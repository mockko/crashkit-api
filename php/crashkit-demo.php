<?php
  define('CRASHKIT_ACCOUNT_AND_PRODUCT', 'test/php');
  define('CRASHKIT_ADMIN_PASSWORD', '12345');
  define('CRASHKIT_DIE_MESSAGE',
    '<title>Server error</title>' .
    '<div style="width: 600px; margin: 50px auto 0px auto; font: 14px Verdana, sans-serif;">' .
    '<b style="color: red;">Server error</b>' .
    '<p>Sorry, we have failed to process your request now. Please try again later.' .
    '<p>Our developers have just been notified about this error.' .
    '<p>If you need help immediately, please e-mail <a href="mailto:test@example.com">test@example.com</a>.' .
    '</div>');
  require_once 'crashkit.inc.php';
?>
<?
  include 'crashkit-demo-included.php';
?>
