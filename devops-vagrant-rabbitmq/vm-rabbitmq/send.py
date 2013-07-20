#!/usr/bin/env python
import pika
import sys

message = ' '.join(sys.argv[1:]) or "Hello World!"

queue = 'jobs'

connection = pika.BlockingConnection(pika.ConnectionParameters(
        host='127.0.0.1'))

channel = connection.channel()

channel.queue_declare(queue=queue, durable=True)

channel.basic_publish(exchange='jobs',
                      routing_key=queue,
                      body=message)

print " [x] Sent %r" % (message,)

connection.close()
