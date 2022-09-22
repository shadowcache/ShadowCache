package alluxio.util;

import alluxio.client.file.cache.PageId;

import java.util.HashMap;


public class LRU {
    public static class Node {
        private PageId item;
        private LRU.Node next;
        private LRU.Node prev;

        Node(LRU.Node prev, PageId pageId, LRU.Node next) {
            this.item = pageId;
            this.next = next;
            this.prev = prev;
        }
        public PageId getItem(){
            return item;
        }

    }
    private LRU.Node head;
    private LRU.Node tail;
    private long size;
    private HashMap<PageId, LRU.Node> idToNode;
    public LRU(){
        size = 0;
        head = null;
        tail = null;
        idToNode = new HashMap<>();
    }
    public PageId peek(){
        return head.item;
    }
    public PageId poll(){
        Node res = head;
        idToNode.remove(head.item);
        head = head.next;
        if(head!=null){
            head.prev = null;
        }
        size--;
        return res.item;
    }
    public boolean get(PageId pageId){
        Node node = idToNode.get(pageId);
        if(node == null){
            return false;
        } else {
            if(node==head){
                head = head.next;
                if(head!=null){
                    head.prev = null;
                }
            }else if(node == tail){
                return true;
            } else{
                node.prev.next = node.next;
                node.next.prev = node.prev;
            }
            node.prev = tail;
            node.next = null;
            tail.next = node;
            tail = node;
            return true;
        }
    }
    public boolean put(PageId pageId){
        if(get(pageId)){
            return true;
        }else{
            size++;
            if(head == null){
                head = new Node(null,pageId,null);
                idToNode.put(pageId,head);
                tail = head;
            }else{
                Node node = new Node(tail,pageId,null);
                idToNode.put(pageId,node);
                tail.next  = node;
                tail = node;
            }
        }
        return true;
    }
    public boolean remove(PageId pageId){
        Node node = idToNode.get(pageId);
        idToNode.remove(node.item);
        if(node==null){
            return false;
        }else{
            if(node==head){
                head = head.next;
                if(head!=null){
                    head.prev = null;
                }
            }else if(node == tail){
                tail = tail.prev;
                tail.next = null;
            } else{
                node.prev.next = node.next;
                node.next.prev = node.prev;
            }
        }
        return true;
    }
    public long getSize(){
        return size;
    }
    public Node getHead(){
        return head;
    }
    public Node next(Node node){
        return node.next;
    }
    public boolean isEmpty() {
        return head == null;
    }
}
