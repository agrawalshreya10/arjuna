'''
This file is a part of Test Mile Arjuna
Copyright 2018 Test Mile Software Testing Pvt Ltd

Website: www.TestMile.com
Email: support [at] testmile.com
Creator: Rahul Verma

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

class ROProxy:
    def __init__(self, wrapped):
        vars(self)['__wrapped'] = wrapped

    def raise_item_absent_exc(item):
        raise Exception("Object does not contain an attribute/item with name: >>{}<<".format(item))

    def __getattr__(self, item):
        try:
            return getattr(vars(self)['__wrapped'], item)
        except AttributeError as e:
            ROProxy.raise_item_absent_exc(item)

    def __getitem__(self, item):
        try:
            return getattr(vars(self)['__wrapped'], item)
        except AttributeError as e:
            ROProxy.raise_item_absent_exc(item)

    def __setattr__(self, key, value):
        raise Exception("Read-Only Proxy does not support item assignment.")

    def __str__(self):
        return str(vars(self)['__wrapped'])