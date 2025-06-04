package com.learningonline.media.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BigFileTest {
    //测试文件分片
    @Test
    public void testChunk() throws IOException {
        //获取源文件
        File sourceFile = new File("D:\\BaiduNetdiskDownload\\01.mp4");
        //创建分片文件的总目录
        String chunkPath = "C:\\Users\\86157\\Desktop\\在线学习平台\\文件测试\\文件分片\\";
        File chunkFolder = new File(chunkPath);
        //确认磁盘中是否存在该路径
        if (!chunkFolder.exists()) {
            chunkFolder.mkdirs();
        }
        //设置分片大小和计算分片数量
        long chunkSize = 1024 * 1024 * 10;//10MB
        long chunkNum = (long) Math.ceil((double) sourceFile.length() / chunkSize);
        //创建数据缓冲区，方便从源文件读取信息
        byte[] dataBuffer = new byte[1024 * 1024];//1MB
        //创建访问文件的IO方法
        RandomAccessFile readSourceFile = new RandomAccessFile(sourceFile, "r");
        for (int i = 0; i < chunkNum; i++) {
            //每个分片文件的对象
            File chunkFile = new File(chunkPath + i);
            if (chunkFile.exists()) {
                chunkFile.delete();
            }
            //在磁盘创建分片文件
            boolean file = chunkFile.createNewFile();
            if (file) {
                //创建写入操作
                RandomAccessFile writeChunkFile = new RandomAccessFile(chunkFile, "rw");
                //创建分片文件，写入数据
                int length = -1;//读取到的字节数,-1代表到文件末尾
                while ((length = readSourceFile.read(dataBuffer)) != -1) {
                    //每次调用 read() 都会隐式文件移动指针，无需手动 seek()
                    writeChunkFile.write(dataBuffer, 0, length);
                    if (chunkFile.length() >= chunkSize) {
                        break;
                    }
                }
                writeChunkFile.close();
                System.out.println("完成分片" + i);
            }
        }
        readSourceFile.close();
    }

    @Test
    public void testMerge() throws IOException {
        //源文件
        File sourceFile = new File("D:\\BaiduNetdiskDownload\\01.mp4");
        //创建合并文件
        File mergeFile = new File("C:\\Users\\86157\\Desktop\\在线学习平台\\文件测试\\分片合并.mp4");
        if (mergeFile.exists()) {
            mergeFile.delete();
        }
        boolean merge = mergeFile.createNewFile();
        if (merge) {
            //创建写操作
            RandomAccessFile writeMergeFile = new RandomAccessFile(mergeFile, "rw");
            writeMergeFile.seek(0);//确保从头写入
            //数据缓冲区
            byte[] dataBuffer = new byte[1024 * 1024];//1MB
            //得到分片文件列表
            String chunkPath = "C:\\Users\\86157\\Desktop\\在线学习平台\\文件测试\\文件分片\\";
            File chunkFolder = new File(chunkPath);
            File[] chunkFiles = chunkFolder.listFiles();
            if (chunkFiles != null) {
                //升序排列
                Arrays.sort(chunkFiles, new Comparator<File>() {
                    public int compare(File o1, File o2) {
                        return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
                    }
                });
                for (File chunkFile : chunkFiles) {
                    RandomAccessFile readChunkFile = new RandomAccessFile(chunkFile, "r");
                    int length = -1;
                    while ((length = readChunkFile.read(dataBuffer)) != -1) {
                        writeMergeFile.write(dataBuffer, 0, length);
                    }
                    readChunkFile.close();
                }
                writeMergeFile.close();
                //验证数据一致
                try (FileInputStream sourceFileInputStream = new FileInputStream(sourceFile);
                     FileInputStream mergeFileInputStream = new FileInputStream(mergeFile);) {
                    String sourceMd5 = DigestUtils.md5Hex(sourceFileInputStream);
                    String mergeMd5 = DigestUtils.md5Hex(mergeFileInputStream);
                    if (sourceMd5.equals(mergeMd5)) {
                        System.out.println("合并成功");
                    } else {
                        System.out.println("合并失败");
                    }
                }
            }
        }

    }

    //测试上传分块到minio
    @Test
    public void testUpload() throws IOException {
        //得到分片列表
        String chunkPath = "C:\\Users\\86157\\Desktop\\在线学习平台\\文件测试\\文件分片\\";
        File chunkFolder = new File(chunkPath);
        File[] chunkFiles = chunkFolder.listFiles();//分片顺序可能不一样
        Arrays.sort(chunkFiles, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });
        if (chunkFiles != null) {
            //上传
            for (int i = 0; i < chunkFiles.length; i++) {
                UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                        .filename(chunkFiles[i].getAbsolutePath())
                        .bucket("testbucket")
                        .object("chunk/" + i).build();
                try {
                    MinioTests.minioClient.uploadObject(uploadObjectArgs);
                    System.out.println("分片:" + i + "上传成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //minio合并测试
    @Test
    public void minioMerge() throws IOException {
        //查询分片
//        List<ComposeSource> sources = new ArrayList<>();
//        for (int i = 0; i < 6; i++) {
//            sources.add(ComposeSource.builder()
//                    .bucket("testbucket")
//                    .object("chunk/" + i)
//                    .build());
//        }
        int chunkNum = 10;
        List<ComposeSource> chunkSources = Stream.iterate(0, i -> ++i).limit(chunkNum)
                .map(i -> ComposeSource.builder()
                        .bucket("testbucket")
                        .object("chunk/" + i).build())
                .collect(Collectors.toList());
        //在minio合并分片
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merger/testMerge.mp4")
                .sources(chunkSources)
                .build();
        try {
            MinioTests.minioClient.composeObject(composeObjectArgs);
            System.out.println("minio合并文件分片成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //minio清楚文件,批量删除
    @Test
    public void testDelete() throws IOException {
        //获取对象流
        List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                .limit(10)
                .map(i -> new DeleteObject("chunk/" + i))
                .collect(Collectors.toList());
        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                .bucket("testbucket")
                .objects(deleteObjects)
                .build();
        Iterable<Result<DeleteError>> results = MinioTests.minioClient.removeObjects(removeObjectsArgs);
        results.forEach(result -> {
                    DeleteError deleteError = null;
                    try {
                        deleteError = result.get();
                        System.out.println(deleteError.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
        System.out.println("清除分片成功");
    }


}
