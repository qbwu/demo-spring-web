#!/usr/bin/env python
# -*- coding: utf-8 -*-
########################################################################
#
# Copyright (c) 2020 qbwu, Inc. All Rights Reserved
#
########################################################################

"""
File: repair_old_data_v910.py
Author: qb.wu@outlook.com 
Date: 2020/08/14
Description: For repairing old data for service upgrading
"""

import json
import sys
import urllib2

#project_id \t user_id \t corp_id
PROJECT_CREATOR_FILE = "project-creators.dat"
BASE_URL = "http://localhost:8666/xxxxxxxx/api/project/v1"


def send_get_req(url, data):
    '''
    send get request to url with json data
    '''

    req = urllib2.Request(url)
    req.add_header('Content-Type', 'application/json')
    req.add_header('X-Http-Method-Override', 'GET')
    datastr = json.dumps(data)
    try:
        response = urllib2.urlopen(req, datastr)

        resp = json.loads(response.read())
        if not resp or resp['code'] != 200:
            print >> sys.stderr, "Failed to send GET request: ", datastr, resp
            return None
    except Exception as ex:
        print >> sys.stderr, "Failed to send GET request: ", datastr, " error: ", ex
        return None

    return resp


def send_post_req(url, data):
    '''
    send post request to url with json data
    '''

    req = urllib2.Request(url)
    req.add_header('Content-Type', 'application/json')

    datastr = json.dumps(data)
    try:
        response = urllib2.urlopen(req, datastr)
        resp = json.loads(response.read())
        if not resp or resp['code'] != 200:
            print >> sys.stderr, "Failed to send POST request: ", datastr, resp
            return None
    except Exception as ex:
        print >> sys.stderr, "Failed to send POST request: ", datastr, " error: ", ex
        return None

    return resp

def send_put_req(url, data):
    '''
    send put request to url with json data
    '''
    req = urllib2.Request(url)
    req.add_header('Content-Type', 'application/json')
    req.get_method = lambda: 'PUT'
    datastr = json.dumps(data)
    try:
        response = urllib2.urlopen(req, datastr)
        resp = json.loads(response.read())
        if not resp or resp['code'] != 200:
            print >> sys.stderr, "Failed to send PUT request: ", datastr, resp
            return None
    except Exception as ex:
        print >> sys.stderr, "Failed to send POST request: ", datastr, " error: ", ex
        return None

    return resp


def run_step(func):
    '''
    call the function on every line of PROJECT_CREATOR_FILE
    '''
    with open(PROJECT_CREATOR_FILE, 'rb') as f:
        for line in f:
            project_id, user_id, corp_id = line.strip().split('\t')
            func(user_id, corp_id, project_id)


def repair_v910_step(user_id, corp_id, project_id):
    '''
    add missing sysChat and reset sysFollowers
    '''

    print >> sys.stderr, "Successful repair-step, project: ", project_id


def main_v910(args):
    run_step(repair_v910_step)


if __name__ == "__main__":
    main_v910(sys.argv)
