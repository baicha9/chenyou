package com.itsq.controller.resources;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itsq.common.base.BaseController;
import com.itsq.common.bean.ErrorEnum;
import com.itsq.common.bean.Response;
import com.itsq.pojo.dto.AddArmsDto;
import com.itsq.pojo.entity.Arms;
import com.itsq.service.resources.ArmsService;
import com.itsq.token.CurrentUser;
import com.itsq.utils.BeanUtils;
import com.itsq.utils.ClientUtils;
import com.itsq.utils.http.Client;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/arms")
@CrossOrigin
@Api(tags = "获取武器相关接口")
@AllArgsConstructor
public class ArmsController  extends BaseController {

    private ArmsService armsService;

    @RequestMapping(value = "getAll",method = RequestMethod.POST)
    @ApiOperation(value = "获取全部武器", notes = "", httpMethod = "POST")
    public Response getAllArms(@RequestBody Arms arms){
        QueryWrapper queryWrapper=new QueryWrapper();
        if(arms.getName()!=null&&arms.getName().length()>0){
            queryWrapper.like("name",arms.getName());
        }
        queryWrapper.orderByDesc("id");
        List<Arms> list=armsService.list(queryWrapper);
        return Response.success(list);
    }

    @RequestMapping(value = "addInfo",method = RequestMethod.POST)
    @ApiOperation(value = "添加武器", notes = "", httpMethod = "POST")
    public Response addInfo(@RequestBody AddArmsDto dto){
        CurrentUser currentUser = currentUser();
        if(currentUser==null){
            return Response.fail(ErrorEnum.SIGN_VERIFI_EXPIRE);
        }
        dto.setCreDate(new Date());
        Arms arms=BeanUtils.copyProperties(dto, Arms.class);
        if(!armsService.save(arms)){
            return Response.fail(ErrorEnum.ERROR_SERVER);
        }
        return Response.success(arms);
    }

    @RequestMapping(value = "romveArms",method = RequestMethod.POST)
    @ApiOperation(value = "删除武器", notes = "", httpMethod = "POST")
    public Response romveArms(@RequestBody String id){
        CurrentUser currentUser = currentUser();
        if(currentUser==null){
            return Response.fail(ErrorEnum.SIGN_VERIFI_EXPIRE);
        }
        JSONObject jsonObject=JSONObject.parseObject(id);
        if(!armsService.removeById(Long.parseLong(jsonObject.getString("id")))){
            return Response.fail(ErrorEnum.ERROR_SERVER);
        }
        return Response.success();
    }

    @RequestMapping(value = "updateById",method = RequestMethod.POST)
    @ApiOperation(value = "修改武器信息", notes = "", httpMethod = "POST")
    public Response updateById(@RequestBody AddArmsDto dto){
        CurrentUser currentUser = currentUser();
        if(currentUser==null){
            return Response.fail(ErrorEnum.SIGN_VERIFI_EXPIRE);
        }
        Arms arms=BeanUtils.copyProperties(dto, Arms.class);
        if(!armsService.updateById(arms)){
            return Response.fail(ErrorEnum.ERROR_SERVER);
        }
        return Response.success(arms);
    }

    @RequestMapping(value = "updateAramStatus",method = RequestMethod.POST)
    @ApiOperation(value = "修改武器状态根据id", notes = "", httpMethod = "POST")
    public Response updateAramStatus(String id,String isStatus){
        CurrentUser currentUser = currentUser();
        if(currentUser==null){
            return Response.fail(ErrorEnum.SIGN_VERIFI_EXPIRE);
        }
        Arms arms=new Arms();
        arms.setId(Integer.parseInt(id));
        arms.setIsStatus(Integer.parseInt(isStatus));
        if(!armsService.updateById(arms)){
            return Response.fail(ErrorEnum.ERROR_SERVER);
        }
        return Response.success();
    }


    //@RequestMapping(value = "addListArms", method = RequestMethod.POST)
    //@ApiOperation(value = "导入武器", notes = "", httpMethod = "POST")
    public Response addListArms() {
        /*CurrentUser currentUser = currentUser();
        if(currentUser==null){
            return Response.fail(ErrorEnum.SIGN_VERIFI_EXPIRE);
        }*/
        System.out.println("=======");
        Map<String,String> param=new HashMap<>();
        String json = ClientUtils.httpGetWithJSon("https://app.zbt.com/open/product/v1/search", param);
        System.out.println("=======");
        Object succesResponse = JSON.parse(json);
        System.out.println("=======");//先转换成Object
        System.out.println(json);
        Map map = (Map)succesResponse;         //Object强转换为Map
        Object succesResponse1 = JSON.parse(map.get("data")+"");
        Map map1 = (Map)succesResponse1;
        List amList=new ArrayList();
        List list= (List)map1.get("list");
      /*  for (Object o : list) {
            Map map2 = (Map)o;
            Arms arms = new Arms();
            arms.setCount((Integer)map2.get("quantity"));
            arms.setImageUrl(map2.get("imageUrl")+"");
            arms.setName(map2.get("itemName")+"");
            arms.setPrice(new BigDecimal(map2.get("price")+""));
            arms.setProductId((Integer)map2.get("itemId"));
            amList.add(arms);
        }
        armsService.addListArms(amList);*/
        return Response.success(list);
    }


}

