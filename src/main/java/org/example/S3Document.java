package org.example;



class S3Document {

        private String key;
        private String content;

        // Constructor
        public S3Document(String key, String content) {
            this.key = key;
            this.content = content;
        }

        // Getter for key
        public String getKey() {
            return key;
        }

        // Setter for key (optional)
        public void setKey(String key) {
            this.key = key;
        }

        // Getter for content
        public String getContent() {
            return content;
        }

        // Setter for content (optional)
        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "S3Document{" +
                    "key='" + key + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }


