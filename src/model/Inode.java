package model;

import java.util.ArrayList;
import java.util.List;

public  class Inode {
    public int id;
    public int sizeBytes;
    public List<Extent> extents;

    public Inode(int id, int sizeBytes) {
        this.id = id;
        this.sizeBytes = sizeBytes;
        this.extents = new ArrayList<>();
    }
}
