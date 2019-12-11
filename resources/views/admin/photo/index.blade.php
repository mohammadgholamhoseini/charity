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
                <a class="fa fa-plus fa-4x" href="{{route('photos.create')}}" style="float: left;"></a>
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
                        <th>نام فایل</th>
                        <th>تاریخ ایجاد</th>
                        <th>جزیات</th>
                    </tr>
                    @foreach($photos as $photo)

                    <tr>
                        <td>{{$photo->id}}</td>
                        <td>{{$photo->original_name}}</td>
                        <td>
                            {{$photo->created_at}}
                        </td>
                        <td>
                                <form method="POST" action="{{route('photos.destroy',[$photo->id])}}">
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
