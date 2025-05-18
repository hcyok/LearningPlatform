package com.learningonline.media.service;


import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;



import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class MinioTests {
    static MinioClient minioClient = MinioClient.builder()
            .endpoint("http://192.168.130.128:9000")
            .credentials("minioadmin", "minioadmin")
            .build();


    //创建bucket
    @Test
    public void makeBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket("testbucket").build());
            if (!found) {
                MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket("testbucket").build();
                minioClient.makeBucket(makeBucketArgs);
                System.out.println("新建成功");
            } else {
                System.out.println("bucket已经存在");
            }
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            System.out.println("创建失败" + e.getMessage());
        }

    }

    //上传文件
    @Test
    public void uploadFile() {
        //根据拓展匹配媒体类型
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".sql");
        String mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mediaType = extensionMatch.getMimeType();
        }
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .object("001/mysql-schema-test001.sql")
                    .filename("C:\\Users\\86157\\Desktop\\在线学习平台\\讲义\\mysql-schema.sql")
                    .contentType(mediaType)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("上传成功");
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void deleteFile() {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket("testbucket")
                    .object("001/mysql-schema-test001.sql")
                    .build());
            System.out.println("删除成功");
        }
        catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
    }
    @Test
    public void downloadFile() {
        try{
            getFile();
            verify();
        }
        catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
    }
    private void getFile() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        GetObjectArgs getObjectArgs=GetObjectArgs.builder().bucket("testbucket")
                .object("mysql-schema-test001.sql").build();
           FilterInputStream inputStream=minioClient.getObject(getObjectArgs);
            FileOutputStream fileOutStream=new FileOutputStream("C:\\Users\\86157\\Desktop\\在线学习平台\\讲义\\test001.sql");
            IOUtils.copy(inputStream,fileOutStream);

    }
    private void verify() throws IOException {
        FileInputStream sourceStream=new FileInputStream("C:\\Users\\86157\\Desktop\\在线学习平台\\讲义\\mysql-schema.sql");
        String source_md5= DigestUtils.md5Hex(sourceStream);
        FileInputStream downdloadStream=new FileInputStream("C:\\Users\\86157\\Desktop\\在线学习平台\\讲义\\test001.sql");
        String downdload_md5= DigestUtils.md5Hex(downdloadStream);
        if(source_md5.equals(downdload_md5)){
            System.out.println("下载成功");
        }
        else{
            System.out.println("下载失败，文件不完整");
        }
    }
}
