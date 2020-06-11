layui.config({
    base: "{/}/templates/lib/layui-v2.5.4/lay/my_modules/"
}).extend({
    layuimini: 'layuimini',
    formSelects: 'formSelects/formSelects-v4',
    treeTable: 'treeTable/treeTable'
}).use(['laypage', 'layer', 'form', 'table', 'laydate', 'element', 'layuimini', 'carousel', 'treeTable'], function () {
    var laypage = layui.laypage,
        layer = layui.layer,
        form = layui.form,
        table = layui.table,
        element = layui.element,
        laydate = layui.laydate,
        layuimini = layui.layuimini,
        carousel = layui.carousel,
        treeTable = layui.treeTable;
    var user = C.getCache('admin');


    var findAjax = function (url, data, success) {
        //console.log(data);
        $.ajax({
            type: "POST",
            url: baseURL + url,
            data: JSON.stringify(data),
            dataType: "JSON",
            contentType: "application/json",
            async: false,
            beforeSend: function (xhr) {
                xhr.setRequestHeader("X-Token", C.find("X-Token"));
            },
            success: function (result) {
                if (result.errorCode + "" === "0") {
                    success(result);
                    layer.closeAll('loading');
                } else {
                    success(result.msg);
                    layer.closeAll('loading');
                }
            },
            error: function (e) {
                layer.msg("系统错误");
                layer.closeAll('loading');
            }
        });
    };


    // 渲染表格
    function initTable(data) {
        //console.log(data);
        var insTb = treeTable.render({
            elem: '#demoTb1',
            data: data,  // 数据
            /*tree: {
                iconIndex: 1, // 折叠图标显示在第几列
                idName: 'menuId',  // 自定义id字段的名称
            },*/
            cols: [
               {
                    field: 'id',
                    title: '武器id',
                    align: 'center',
                    width: 150
                }, {
                    field: 'name',
                    title: '饰品名称',
                    edit: 'text',
                    align: 'center'
                },
                {   field: 'type',
                    title: '饰品品质',
                    align: 'center',
                    templet: function (d) {
                        var strs = [
                            '<span></span>',
                            '<span class="layui-badge layui-badge-green">消费级</span>',
                            '<span class="layui-badge layui-badge-green">工业级</span>',
                            '<span class="layui-badge layui-badge-green">军规级</span>',
                            '<span class="layui-badge layui-badge-green">受限级</span>',
                            '<span class="layui-badge layui-badge-green">保密级</span>',
                            '<span class="layui-badge layui-badge-green">隐秘级</span>',
                            '<span class="layui-badge layui-badge-green">特殊物品*</span>',
                        ];
                        return strs[d.type];
                    }
                }
                ,{
                    field: 'imageUrl',
                    title: '饰品主图',
                    align: 'center',
                    templet: function(d) {
                        var url="img/"+d.type+".png";
                        var htmlInfo= '<span class="backImg" style="background-image: url(\''+url+'\');display: inline-block;\n' +
                            '    background-repeat: no-repeat;\n' +
                            '    background-size: contain;\n' +
                            '    width: 100px;\n' +
                            '    height: 100px;"><img ] src="'+d.imageUrl+'" width="50"height="50"/></span>'
                        return htmlInfo;
                    }
                }, {
                    field: 'price',
                    title: '价格',
                    align: 'center',
                    edit: 'text'
                }, {
                    field: 'count',
                    title: '库存',
                    align: 'center',
                    edit: 'text'
                }, {
                    field: 'productId',
                    title: '第三方武器ID',
                    align: 'center',
                    edit: 'text'
                },
                {
                    title: '状态',
                    align: 'center',
                    templet: '#switchTpl',
                    unresize: true
                },
                {
                    title: '操作',
                    width: 200,
                    fixed: 'right',
                    align: 'center',
                    templet: function (d) {
                        return "<button class='layui-btn layui-btn-xs layui-btn-normal' lay-event='updatenavi'>编辑</button>" +
                            "<button class='layui-btn layui-btn-xs layui-btn-warm' lay-event='delnavi'>删除</button>";
                    }
                }
            ]
        });

    }


    function changeList(list) {
        initTable(list);
    }


    function show() {
        var list = [];
        findAjax("arms/getAll", {}, function (res) {
            var datas = res.body;
            for (var i = 0; i < datas.length; i++) {
                list = list.concat(datas[i]);
            }
            changeList(list);

        });
    }


    show();


    $('[lay-filter="addSysmenu"]').on('click', function (obj) {
        layer.open({
            type: 1,
            title: "添加武器",
            id: "alert",
            area: ['500px', '500px'],
            content: $("#addArms").html(),
            success: function (layero, index) {
                layui.use(['form','upload'],function() {
                    var $ = layui.jquery,upload = layui.upload;
                    //普通图片上传
                    var uploadInst = upload.render({
                        elem: '#test1'
                        , url: baseURL + 'api/file/upload' //改成您自己的上传接口
                        , before: function (obj) {
                            //预读本地文件示例，不支持ie8
                            obj.preview(function (index, file, result) {
                                $('#imageUrl').attr('src', result); //图片链接（base64）
                            });
                        }
                        , done: function (res) {
                            //如果上传失败
                            if (res.code > 0) {
                                return layer.msg('上传失败');
                            }
                            //上传成功
                            $('#imageUrl').attr('src', baseURL + res.body.filePathURL);
                        }
                        , error: function () {
                            //演示失败状态，并实现重传
                            var demoText = $('#demoText');
                            demoText.html('<span style="color: #FF5722;">上传失败</span> <a class="layui-btn layui-btn-xs demo-reload">重试</a>');
                            demoText.find('.demo-reload').on('click', function () {
                                uploadInst.upload();
                            });
                        }
                    });
                });

                form.render('select');
                $("#commit").on('click', function (obj) {
                    var name = $(" [name='name']").val();
                    var imageUrl = $(" #imageUrl").attr("src");
                    var price = $(" [name='price']").val();
                    var count = $(" [name='count']").val();
                    var isStatus = $(" [name='isStatus']").val();
                    var productId=$("[name='productId']").val();
                    var type=$("[name='type']").val();
                    findAjax("arms/addInfo", {
                        "name": name,
                        "imageUrl": imageUrl,
                        "price": price,
                        "count": count,
                        "isStatus": isStatus,
                        "type" : type,
                        "productId":productId
                    }, function (res) {
                        if (res.message == 'success') {
                            layer.msg("添加成功", function () {
                                setTimeout($.ajax({
                                    type: "POST",
                                    url: baseURL + "arms/getAll",
                                    data: {
                                        adminId: user.id
                                    },
                                    dataType: "json",
                                    success: function () {
                                    }
                                }), 100);

                                layer.close(index);
                                show();
                            });
                        }
                    });
                });
                $("#cancle").on('click', function (obj) {
                    layer.close(index);
                });

            }
        });
    });


    form.on('switch(sources)', function (obj) {
        //layer.tips(this.value + ' ' + this.name + '：'+ obj.elem.checked, obj.othis);
        if (obj.elem.checked) {
            //恢复操作
            $.ajax({
                type: "post",
                url: baseURL + "arms/updateAramStatus",
                data: {
                    id: this.value,
                    "isStatus": "0"
                },
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("X-Token", C.find("X-Token"));
                },
                success: function (res) {
                    if (res.message == 'success') {
                        layer.msg('恢复成功');
                        show();
                    }
                }
            });
        } else {
            //冻结操作
            $.ajax({
                type: "post",
                url: baseURL + "arms/updateAramStatus",
                data: {
                    "id": this.value,
                    "isStatus": "1"
                },
                beforeSend: function (xhr) {
                    xhr.setRequestHeader("X-Token", C.find("X-Token"));
                },
                success: function (res) {
                    if (res.message == 'success') {
                        layer.msg('冻结成功');
                        show();
                    }
                }
            });
        }
    });

    treeTable.on('tool(demoTb1)', function (obj) {
        var data = obj.data;  // 获得当前行数据
        var layEvent = obj.event; // 获得lay-event对应的值
        if (layEvent === 'delnavi') {

            layer.confirm("确认删除此武器吗?", function () {
                findAjax("arms/romveArms", {"id":data.id}, function (res) {
                    if (res.message == 'success') {
                        layer.msg('删除成功！');

                        show();
                    }
                })
            });

        } else if (layEvent === 'updatenavi') {
            //console.log(data);
                    layer.confirm("确认编辑吗?", function () {
                        findAjax("arms/updateById", {
                            "id":data.id,
                            "name": data.name,
                            "imageUrl": data.imageUrl,
                            "price": data.price,
                            "count": data.count,
                            "productId":data.productId
                        }, function (res) {
                            if (res.message == 'success') {
                                layer.msg('编辑成功');
                                setTimeout($.ajax({
                                    type: "POST",
                                    url: baseURL + "arms/getAll",
                                    data: {
                                        adminId: user.id
                                    },
                                    dataType: "json",
                                    success: function () {
                                    }
                                }), 100);

                                layer.close(index);
                                show();
                            }
                        })
                    });
        }
    });


});