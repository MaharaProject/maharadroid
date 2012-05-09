<?php
/**
 * Mahara: Electronic portfolio, weblog, resume builder and social networking
 * Copyright (C) 2006-2009 Catalyst IT Ltd and others; see:
 *                         http://wiki.mahara.org/Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @package    mahara
 * @subpackage artefact-file, artefact-blog
 * @author     Catalyst IT Ltd
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL
 * @copyright  (C) 2006-2009 Catalyst IT Ltd http://catalyst.net.nz
 *
 */

define('INTERNAL', 1);
define('PUBLIC', 1);

require(dirname(dirname(dirname(__FILE__))) . '/init.php');
safe_require('artefact', 'file');
safe_require('artefact', 'blog');

$json = array();

if (!get_config('allowmobileuploads')) {
    jsonreplyfinal(array('fail' => 'Mobile uploads disabled'));
}

$token = '';
try {
    $token = param_variable('token');
    $token = trim($token);
}
catch (ParameterException $e) { }

if ($token == '') {
    jsonreplyfinal(array('fail' => 'Auth token cannot be blank'));
}

$username = '';
try {
    $username = trim(param_variable('username'));
}
catch (ParameterException $e) { }

if ($username == '') {
    jsonreplyfinal(array('fail' => 'Username cannot be blank'));
}

$USER = new User();

try {
    $USER->find_by_mobileuploadtoken($token, $username);
}
catch (AuthUnknownUserException $e) {
    jsonreplyfinal(array('fail' => 'Invalid user token'));
}

// error_log(var_dump($USER));

// Add in bits of sync data - let's start with notifications
$lastsync = time();
try {
    $lastsync = param_variable('lastsync') + 0;
}
catch (ParameterException $e) { }

$activity = get_records_sql_array('select n.id, n.subject, n.message 
					from {notification_internal_activity} n, {activity_type} a
					where n.type=a.id and n.read=0 and '
					. db_format_tsfield('n.ctime', '') . ' >= ? and n.usr= ? ', 
					array($lastsync + 0, $USER->id));
if ( $activity ) 
  $json['activity'] = $activity;

// OK - let's add tags


$tagsort = param_alpha('ts', null) != 'freq' ? 'alpha' : 'freq';
$tags = get_my_tags(null, false, $tagsort);

if ( $tags ) 
  $json['tags'] = $tags;

// OK - let's add journals

$blogs = (object) array(
    'offset' => param_integer('offset', 0),
    'limit'  => param_integer('limit', 10),
);

list($blogs->count, $blogs->data) = ArtefactTypeBlog::get_blog_list($blogs->limit, $blogs->offset);

//foreach ( $blogs->data  as $blog ) {
//  unset($blog['deletefrom']); // just because it's large
//}

if ( $blogs->data ) 
  $json['blogs'] = $blogs->data;

// OK - let's add folders

$folders = ArtefactTypeFile::get_my_files_data(0, $USER->id, null, null, array("artefacttype" => array("folder")));

if ( $folders ) 
  $json['folders'] = $folders;

// Here we need to create a new hash - update our own store of it and return it too the handset
jsonreplyfinal ( array("success" => $USER->refresh_mobileuploadtoken() ));

function jsonreplyfinal ( $arr ) {
  global $json;
  if ( $json ) 
    $arr['sync'] = $json;
  header('Content-Type: application/json');
  echo json_encode($arr);
  exit;
}

