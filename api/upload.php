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
 * @subpackage artefact-file
 * @author     Catalyst IT Ltd
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL
 * @copyright  (C) 2006-2009 Catalyst IT Ltd http://catalyst.net.nz
 *
 */

define('INTERNAL', 1);
define('PUBLIC', 1);
define('SECTION_PLUGINTYPE', 'artefact');
define('SECTION_PLUGINNAME', 'blog');

$json = array();

require(dirname(dirname(dirname(__FILE__))) . '/init.php');
safe_require('artefact', 'file');
safe_require('artefact', 'blog');

if (!get_config('allowmobileuploads')) {
    jsonreply( array('fail' => 'Mobile uploads disabled') );
}

$token = '';
try {
    $token = param_variable('token');
    $token = trim($token);
}
catch (ParameterException $e) { }

if ($token == '') {
    jsonreply( array('fail' => 'Auth token cannot be blank') );
}

$username = '';
try {
    $username = trim(param_variable('username'));
}
catch (ParameterException $e) { }

if ($username == '') {
    jsonreply( array('fail' => 'Username cannot be blank') );
}

// Starting the creation of our artefact (file) object
$data = new StdClass;
$USER = new User();

try {
    $USER->find_by_mobileuploadtoken($token, $username);
}
catch (AuthUnknownUserException $e) {
    jsonreply( array('fail' => 'Invalid user token') );
}

$data->owner = $USER->get('id'); // id of owner

$folder = '';
try {
    $folder = param_variable('foldername');
    $folder = trim($folder);

    if ($folder) {
        // TODO: create if doesn't exist - note assumes it is a base folder (hence null parent)
        $artefact = ArtefactTypeFolder::get_folder_by_name($folder, null, $data->owner);  // id of folder you're putting the file into
        if ($artefact) {
            $data->parent = $artefact->id;
            if ($data->parent == 0) {
                $data->parent = null;
            }
        }
        else {
            $fd = (object) array(
                'owner' => $data->owner,
                'title' => $folder,
                'parent' => null,
            );
            $f = new ArtefactTypeFolder(0, $fd);
            $f->commit();
            $data->parent = $f->get('id');
        }
    }
    else {
        $data->parent = null;
    }
}
catch (ParameterException $e) {
    $data->parent = null;
}

// Check for Journal ID to add a post to
$blog = ''; $blogpost = ''; $draft = 0; $allowcomments = 1;
try {
    $blog = param_integer('blog');
}
catch (ParameterException $e) { }
try {
	$blogpost = param_integer('blogpost');
}
catch (ParameterException $e) {
}
try {
    $draft = param_variable('draft');
}
catch (ParameterException $e) { }

try {
    $allowcomments = param_variable('allowcomments');
}
catch (ParameterException $e) { }

// Check for artefact attributes title, description (or entry), tags, etc
$title = ''; $description = ''; $tags = ''; 
try {
    $title = param_variable('title');
}
catch (ParameterException $e) { }

// Set description
try {
    $description = param_variable('description');
}
catch (ParameterException $e) { }

// Set tags
try {
    $tags = explode(",", param_variable('tags'));
}
catch (ParameterException $e) { }

// -- Now check for files to upload --
$artefact_id = '';  // our resulting artefact id on creation

if ( $_FILES ) {

    $file_title = $title;
    if ( $blog || ! $title ) {  // set the filename to be the title of the artefact
        $file_title = basename($_FILES['userfile']['name']);
    }

    try {
        $data->title = ArtefactTypeFileBase::get_new_file_title($file_title, $data->parent, $data->owner);
        if ( ! $blog ) { // only set a description if it's an artefact upload
          $data->description = $description;
        }
        $data->tags = $tags;
        $artefact_id = ArtefactTypeFile::save_uploaded_file('userfile', $data);
    }
    catch (QuotaExceededException $e) {
        jsonreply( array('fail' => 'Quota exceeded' ) );
    }
    catch (UploadException $e) {
        jsonreply( array('fail' => 'Failed to save file') );
    }
}

// -- Next create a blog entry --
$postobj = '';      // our resulting blog post object on creation

if ( $blog ) {
    if (!get_record('artefact', 'id', $blog, 'owner', $USER->get('id'))) {
        // Blog security is also checked closer to when blogs are added, this 
        // check ensures that malicious users do not even see the screen for 
        // adding a post to a blog that is not theirs
        throw new AccessDeniedException(get_string('youarenottheownerofthisblog', 'artefact.blog'));
    }
    // Should we create a new post or attach the new file to an existing post
    $postids = get_records_sql_array("
        SELECT a.id
        FROM {artefact} a
        WHERE a.title = ? AND a.description = ?
	AND a.owner = ? ",
        array($title, $description, $USER->get('id')));
    if ( $postids ) {
        $blogpost = $postids[0]->id;
        $postobj = new ArtefactTypeBlogPost($blogpost);
        $postobj->check_permission();
        if ($postobj->get('locked')) {
            throw new AccessDeniedException(get_string('submittedforassessment', 'view'));
        }
    } else {
        $postobj = new ArtefactTypeBlogPost(null, null);
        $postobj->set('title', $title);
        $postobj->set('description', $description);
        $postobj->set('tags', $tags);
        $postobj->set('published', !$draft);
        $postobj->set('allowcomments', (int) $allowcomments);
        $postobj->set('parent', $blog);
        $postobj->set('owner', $USER->id);
        $postobj->commit();
        $blogpost = $postobj->get('id');
    }
} else if ( $blogpost ) {
	$postobj = new ArtefactTypeBlogPost($blogpost);
	$postobj->check_permission();
	if ($postobj->get('locked')) {
		throw new AccessDeniedException(get_string('submittedforassessment', 'view'));
	}
}
if ( $blogpost ) {
	$json['id'] = $blogpost;
}

// Check to see if we're creating a journal entry

// -- Finally attach the file to the blog post once uploaded and --

if ( $artefact_id && $postobj ) {
    // If we created or matched a blog post and created an artefact
    // attach the artefact to the blog.
    $postobj->attach($artefact_id);
}

// Here we need to create a new hash - update our own store of it and return it to the handset
jsonreply( array('success' => $USER->refresh_mobileuploadtoken($token) );

function jsonreply( $arr ) {
  global $json;
  if ( $json )
    $arr['sync'] = $json;
  header('Content-Type: application/json');
  echo json_encode($arr);
  exit;
}

