@extends('admin.layout.master')
@section('content')
    @if (\Illuminate\Support\Facades\Session::has('success'))
        <div class="alert alert-success">
            <div>{{session('success')}}</div>
        </div>
    @endif

    @if (\Illuminate\Support\Facades\Session::has('delete'))
        <div class="alert alert-danger">
            <div>{{session('delete')}}</div>
        </div>
    @endif
    <section class="content-header">

        <h1>
            دسته بندی ها
        </h1>
        <ol class="breadcrumb">
            <div style="position: relative; margin-top: -14px; margin-left:47px">
                <a class="fa fa-plus fa-4x" href="{{route('categories.create')}}" style="float: left;"></a>
            </div>
        </ol>
    </section>
    <section class="content">
        <div class="box">

            <!-- /.box-header -->
            <div class="box-body no-padding">
                <table class="table table-striped">
                    <tbody><tr>
                        <th>#</th>
                        <th>نام دسته بندی</th>
                        <th>توضیحات</th>
                        <th>جزیات</th>
                    </tr>
                    @foreach($categories as $category)

                    <tr>
                        <td>{{$category->id}}</td>
                        <td>{{$category->name}}</td>
                        <td>
                            {{Str::limit($category->description,70)}}
                        </td>
                        <td>

                                <a class="btn btn-warning" href="{{route('categories.edit',[$category->id])}}" style="float: right; margin-left: 2px; ">ویرایش</a>
                                <form method="POST" action="{{route('categories.destroy',[$category->id])}}">
                                    @csrf
                                    <input type="hidden" name="_method" value="Delete" >

                                    <button type="submit" class="btn btn-danger" style="width: 65px; ">حذف</button>

                                </form>

                        </td>
                    </tr>
                    @endforeach
                    </tbody></table>
            </div>
            <!-- /.box-body -->
        </div>

    </section>
@endsection
