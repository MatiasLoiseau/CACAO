/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.conf;

import org.springframework.cloud.stream.binder.PartitionKeyExtractorStrategy;
import org.springframework.cloud.stream.binder.PartitionSelectorStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 * Configuration related to use of CloudStream
 * 
 * @author Gustavo Figueiredo
 *
 */
@Configuration
public class StreamConfiguration {
	
	/**
	 * Property name that may be set as 'header' of a 'Message' in order to determine the
	 * exact partition of KAFKA.
	 * It's also important to set the application property 'spring.cloud.stream.bindings.<binding-name>.producer.partition-count'
	 * to some value greater than this number. Otherwise this will be useless and only the partition 0 will be used.
	 */
	public static final String HEADER_PARTITION = "partitionId";
	
	/**
	 * Wrapper class for setting the exact partition number to be used in produced message
	 * @author Gustavo Figueiredo
	 */
	private static class Partition {
		private final int partition;
		Partition(int partition) {
			this.partition = partition;
		}
		public String toString() {
			return String.valueOf(partition);
		}
	}
	
	/**
	 * Bean configured as application property 'spring.cloud.stream.bindings.<binding-name>.producer.partition-key-extractor-name' to
	 * provide a key out of a produced message. This key will be used later for determining the partition.
	 */
	@Bean("UploadedFilePartitioner")
	public PartitionKeyExtractorStrategy partitionKeyExtractorStrategy() {
		return new PartitionKeyExtractorStrategy() {

			/**
			 * If the message includes the header 'partitionId', use this number for determining the exact partition. Otherwise
			 * use the payload as 'key' (if it's a String or a Number) or the hashcode (if it's not). 
			 */
			@Override
			public Object extractKey(Message<?> message) {
				Integer partition = (Integer)message.getHeaders().get(HEADER_PARTITION);
				if (partition!=null)
					return new Partition(partition);
				
				Object payload = message.getPayload();
				if ((payload instanceof String) || (payload instanceof Number)) {
					return payload;
				}
				return payload.hashCode();
				
			}
			
		};
	}
	
	/**
	 * Bean configured as application property 'spring.cloud.stream.bindings.<binding-name>.producer.partition-selector-name' to provide
	 * a partition number out of a key.
	 */
	@Bean("UploadedFileSelector")
	public PartitionSelectorStrategy partitionSelectorStrategy() {
		return new PartitionSelectorStrategy() {

			/**
			 * If the key is an instance of 'Partition', use this information as the partition to be returned (capped to the
			 * maximum configured as application property 'spring.cloud.stream.bindings.<binding-name>.producer.partition-count')
			 */
			@Override
			public int selectPartition(Object key, int partitionCount) {
				if (key instanceof Partition)
					return ((Partition)key).partition % partitionCount;

				else if (key==null)
					return 0;
				
				else
					return key.hashCode() % partitionCount;
			}
			
		};
	}
}
