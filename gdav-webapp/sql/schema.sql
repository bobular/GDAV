-- MySQL dump 10.10
--
-- Host: localhost    Database: tempalbi
-- ------------------------------------------------------
-- Server version	5.0.21-max-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `alignment`
--

DROP TABLE IF EXISTS `alignment`;
CREATE TABLE `alignment` (
  `model_id` int(11) NOT NULL,
  `alignment_id` int(11) NOT NULL auto_increment,
  `spp` varchar(100) NOT NULL,
  `chr` varchar(30) NOT NULL,
  `start` int(11) NOT NULL,
  `end` int(11) NOT NULL,
  `strand` int(11) NOT NULL default '0',
  `score` float NOT NULL default '0',
  `method` varchar(100) default NULL,
  `cigar` varchar(100) default NULL,
  PRIMARY KEY  (`alignment_id`),
  KEY `spp` (`spp`),
  KEY `chr` (`chr`),
  KEY `start` (`start`),
  KEY `end` (`end`),
	KEY `model` (`model_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `annotation`
--

DROP TABLE IF EXISTS `annotation`;
CREATE TABLE `annotation` (
  `model_id` int(11) NOT NULL,
  `submission_id` int(11) NOT NULL,
  `col_id` int(11) NOT NULL,
  `row_id` int(11) NOT NULL auto_increment,
  `annotation_value` varchar(250) default NULL,
  PRIMARY KEY  (`model_id`,`submission_id`,`col_id`,`row_id`),
	KEY `sub_id` (`submission_id`),
  KEY `col_row` (`col_id`,`row_id`),
  FULLTEXT KEY `annotation_value` (`annotation_value`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `col`
--

DROP TABLE IF EXISTS `col`;
CREATE TABLE `col` (
  `submission_id` int(11) NOT NULL,
  `col_id` int(11) NOT NULL auto_increment,
  `title` varchar(100) NOT NULL,
  PRIMARY KEY  (`submission_id`, `col_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `model`
--

DROP TABLE IF EXISTS `model`;
CREATE TABLE `model` (
  `model_id` int(11) NOT NULL auto_increment,
  `model_name` varchar(30) NOT NULL,
  `spp` varchar(100) default NULL,
  `description` varchar(250) default NULL,
  `sequence` text,
  PRIMARY KEY  (`model_id`),
	KEY `name` (`model_name`),
  FULLTEXT KEY `model_name` (`model_name`,`spp`,`description`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `submission`
--

DROP TABLE IF EXISTS `submission`;
CREATE TABLE `submission` (
  `submission_id` int(11) NOT NULL auto_increment,
  `description` text,
  `submitted` timestamp NOT NULL default CURRENT_TIMESTAMP,
  PRIMARY KEY  (`submission_id`),
  FULLTEXT KEY `description` (`description`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

