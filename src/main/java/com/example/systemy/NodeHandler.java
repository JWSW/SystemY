//package com.example.systemy;
//
//import java.io.*;
//import java.net.*;
//import java.util.*;
//
//public class NodeHandler implements Runnable {
//    private Socket clientSocket;
//    private Node node;
//    private DataInputStream in;
//    private DataOutputStream out;
//
//    public NodeHandler(Socket socket, Node node) {
//        this.clientSocket = socket;
//        this.node = node;
//    }
//
//    @Override
//    public void run() {
//        try {
//            in = new DataInputStream(clientSocket.getInputStream());
//            out = new DataOutputStream(clientSocket.getOutputStream());
//
//            String requestType = in.readUTF();
//            switch (requestType) {
//                case "getSuccessor":
//                    handleGetSuccessorRequest();
//                    break;
//                case "getPredecessor":
//                    handleGetPredecessorRequest();
//                    break;
//                case "notify":
//                    handleNotifyRequest();
//                    break;
//                case "findSuccessor":
//                    handleFindSuccessorRequest();
//                    break;
//                case "addFile":
//                    handleAddFileRequest();
//                    break;
//                case "getFileOwner":
//                    handleGetFileOwnerRequest();
//                    break;
//                case "removeFile":
//                    handleRemoveFileRequest();
//                    break;
//                default:
//                    System.err.println("Invalid request type from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
//            }
//
//            clientSocket.close();
//        } catch (IOException e) {
//            System.err.println("Error handling client request: " + e.getMessage());
//        }
//    }
//
//    private void handleGetSuccessorRequest() throws IOException {
//        int successor = node.getSuccessorId();
//        out.writeInt(successor);
//    }
//
//    private void handleGetPredecessorRequest() throws IOException {
//        int predecessor = node.getPredecessorId();
//        out.writeInt(predecessor);
//    }
//
//    private void handleNotifyRequest() throws IOException {
//        int predecessor = in.readInt();
//        node.notify(predecessor);
//    }
//
//    private void handleFindSuccessorRequest() throws IOException {
//        String filename = in.readUTF();
//        int successor = node.getSuccessorId(filename);
//        out.writeInt(successor);
//    }
//
//    private void handleAddFileRequest() throws IOException {
//        String filename = in.readUTF();
//        int fileHash = node.getHash(filename);
//        int successor = node.getSuccessorId(filename);
//        node.addFile(filename, successor);
//        fileMap.put(filename, successor);
//        out.writeBoolean(true);
//    }
//
//    private void handleGetFileOwnerRequest() throws IOException {
//        String filename = in.readUTF();
//        int owner = fileMap.getOrDefault(filename, -1);
//        out.writeInt(owner);
//    }
//
//    private void handleRemoveFileRequest() throws IOException {
//        String filename = in.readUTF();
//        int fileHash = node.getHash(filename);
//        int owner = fileMap.getOrDefault(filename, -1);
//        if (owner != -1) {
//            node.removeFile(filename, owner);
//            fileMap.remove(filename);
//            out.writeBoolean(true);
//        } else {
//            out.writeBoolean(false);
//        }
//    }
//}
