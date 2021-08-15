package com.ariseontech.joindesk.auth.web;

import com.ariseontech.joindesk.HelperUtil;
import com.ariseontech.joindesk.SystemInfo;
import com.ariseontech.joindesk.exception.ErrorCode;
import com.ariseontech.joindesk.exception.JDException;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

@RestController
public class MediaController {

    @Value("${upload-dir}")
    private String uploadPath;
    @Autowired
    private HelperUtil helperUtil;

    @RequestMapping(value = SystemInfo.apiPrefix + "/media/{id:.+}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getMedia(@PathVariable("id") String id) {
        byte[] fileContent;
        try {
            fileContent = FileUtils.readFileToByteArray(new File(helperUtil.getDataPath(uploadPath) + id));
        } catch (IOException e) {
            throw new JDException("", ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(fileContent);
    }
}
