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
            خیریه ها
        </h1>
        <ol class="breadcrumb">
            <div style="position: relative; margin-top: -14px; margin-left:47px">
                <a class="fa fa-plus fa-4x" href="{{route('charities.create')}}" style="float: left;"></a>
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
                        <th>نام خیریه</th>
                        <th>توضیحات</th>
                        <th>جزیات</th>
                    </tr>
                    @foreach($charities as $charity)

                    <tr>
                        <td>{{$charity->id}}</td>
                        <td>{{$charity->name}}</td>
                        <td>
                            {{Str::limit($charity->description,70)}}
                        </td>
                        <td>

                                <a class="btn btn-warning" href="{{route('categories.edit',[$charity->id])}}" style="float: right; margin-left: 2px; ">ویرایش</a>
                                <form method="POST" action="{{route('categories.destroy',[$charity->id])}}">
                                    @csrf
                                    <input type="hidden" name="_method" value="Delete" >

                                    <button type="submit" class="btn btn-danger" style="width: 65px; ">حذف</button>

                                </form>

                        </td>
                    </tr>
                    @endforeach
                    </tbody></table>
                {{ $charities->links() }}
            </div>
            <!-- /.box-body -->
        </div>

    </section>
@endsection
