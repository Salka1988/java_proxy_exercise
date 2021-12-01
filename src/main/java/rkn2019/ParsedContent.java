package rkn2019;

class ParsedContent {

    ParsedContent() {}

    boolean parseContent(byte[] contentInBytes) {
        return true;
    }

    String getHostName() {
        // TODO parse host
        return "HOST";
    }

    int getPortNumber() {
        // TODO parse port number
        return 80;
    }

    byte[] getParsedMessage() {
        // TODO parse message
        return new byte[2];
    }
}
