package org.ssochi.grainspace;


import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class GrainSpace {
    //地图长
    private int len,light;
    //粒子最大重量
    final static int maxW = 16581375;
    private static final Random r = new Random();
    //下次需要更新的粒子
    private List<Grain> grains;
    //用于显示的plane
    private PixelPlane plane;
    //当前使用的放当前帧粒子的矩阵
    private Grain[][] cur;
    //下一帧放粒子的矩阵
    private Grain[][] next;
    //动态添加的粒子
    private List<Grain> addition;
    int[][] cacheArray;

    GrainSpace(int len,int light){
        this.len = len;
        this.light = light;
        cur = new Grain[len][len];
        next = new Grain[len][len];
        cacheArray = new int[len][len];
        grains = new LinkedList<>();
        addition = new LinkedList<>();
        grains.add(new Grain(2,maxW,len / 2,len / 2,len));

        plane = new PixelPlane(len,len);
        plane.boot();

        //动态添加
        Thread t = new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (sc.hasNext()){
                int x = sc.nextInt();
                int y = sc.nextInt();
                int w = sc.nextInt();
                int d = sc.nextInt();
                System.out.println(String.format("input = %d %d %d %d",x,y,w,d));
                addition.add(new Grain(d,w,x,y,len));
            }
        });
        t.start();
    }

    public void run() throws InterruptedException {
        //noinspection InfiniteLoopStatement
        while (true){
            checkDivide();
            updatePlane(light);
            swapGrainMap();
            grains.addAll(addition);
        }
    }

    /**
     * 交换两个矩阵,清空旧的
     */
    private void swapGrainMap() {
        Grain[][] tmp = next;
        next = cur;
        cur = tmp;

        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                next[i][j] = null;
            }
        }
        grains.clear();
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                if (cur[i][j] != null){
                    grains.add(cur[i][j]);
                }
            }
        }
    }

    /**
     * 更新图像
     * @param light 粒子亮度的放大倍数，light较大时能看到更多细节
     * @throws InterruptedException
     */
    private void updatePlane(int light) throws InterruptedException {
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                Grain grain = next[i][j];
                if (grain != null){
                    cacheArray[i][j] = grain.w * light;
                }
            }
        }
        plane.update(new PixelInfo(cacheArray));
    }

    /**
     * 将生成的粒子放入下一个矩阵,如果发生碰撞则计算碰撞
     */
    private void checkDivide() {
        for (Grain grain : grains) {
            if (grain.canDivide()){
                for (Grain g : grain.divide()) {
                    putMap(next,g);
                }
            }else{
                grain.move();
                putMap(next,grain);
            }
        }
    }

    /**
     * 将粒子放入一个矩阵
     * @param next 矩阵
     * @param g  粒子
     */
    private static void putMap(Grain[][] next, Grain g) {
        if (next[g.x][g.y] == null){
            next[g.x][g.y] = g;
            return;
        }
        Grain cur = next[g.x][g.y];
        cur.collide(g);
    }
    //粒子类
    static class Grain{
        int direct;
        int w;
        int x,y;
        int len;

        public Grain(int direct, int w, int x, int y,int len) {
            this.direct = direct;
            this.w = w;
            this.x = x;
            this.y = y;
            this.len = len;
        }

        public boolean canDivide(){
            if (w <= 1)
                return false;
            double rate = 1d - Math.pow(2,-w);
            return r.nextDouble() < rate;
        }
        public List<Grain> divide(){

            int d1,d2;
            if (direct == 8 || direct == 2){
                d1 = 4;d2 = 6;
            }else{
                d1 = 8;d2 = 2;
            }

            List<Grain> res = new LinkedList<>();
            res.add(new Grain(d1,w / 2,x,y,len));
            res.add(new Grain(d2,w - w / 2,x,y,len));
            res.forEach(Grain::move);

            return res;
        }
        public void move(){
            switch (direct){
                case 2:
                    y--;
                    break;
                case 4:
                    x--;
                    break;
                case 6:
                    x++;
                    break;
                case 8:
                    y++;
                    break;

            }
            if (y > len - 1) y = 0;
            if (x > len - 1) x = 0;
            if (y < 0) y = len - 1;
            if (x < 0) x = len - 1;
        }
        public void collide(Grain in){
            direct = in.w > w ? in.direct : direct;
            w += in.w;
        }
    }
}

